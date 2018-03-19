package xdean.csv.fluent;

import static java.lang.String.format;
import static xdean.jex.util.lang.ExceptionUtil.uncatch;
import static xdean.jex.util.lang.ExceptionUtil.uncheck;
import static xdean.jex.util.task.TaskUtil.firstNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.reactivex.Flowable;
import xdean.csv.CSV;
import xdean.csv.CsvColumn;
import xdean.csv.CsvConfig;
import xdean.csv.CsvException;
import xdean.csv.CsvReader;
import xdean.csv.CsvValueParser;
import xdean.jex.extra.function.ActionE2;
import xdean.jex.log.Logable;
import xdean.jex.util.OptionalUtil;
import xdean.jex.util.lang.PrimitiveTypeUtil;
import xdean.jex.util.reflect.ReflectUtil;
import xdean.jex.util.string.StringUtil;

public class FluentCsv implements CsvConfig, Logable {
  private final List<CsvColumn<?>> columns = new ArrayList<>();
  private String splitor = ",";

  @Override
  public List<CsvColumn<?>> columns() {
    return columns;
  }

  @Override
  public CsvConfig splitor(String splitor) {
    this.splitor = splitor;
    return this;
  }

  @Override
  public CsvConfig addColumn(CsvColumn<?> column) {
    findColumn(column.name())
        .ifPresent(c -> Util.throwIt(new IllegalArgumentException("Column " + column.name() + " already exists.")));
    columns.add(column);
    return this;
  }

  @Override
  public CsvReader<List<Object>> asList() {
    return new FluentReader();
  }

  @Override
  public <T> CsvReader<T> asBean(Class<T> bean, UnaryOperator<BeanResultConfig<T>> config) {
    try {
      BeanConstructor<T> con = new BeanConstructor<>(bean, config);
      return asList().map(s -> con.construct(s));
    } catch (CsvException e) {
      return f -> Flowable.error(e);
    }
  }

  private Optional<CsvColumn<?>> findColumn(String name) {
    return columns.stream()
        .filter(c -> Objects.equals(c.name(), name))
        .findAny();
  }

  private void assertTrue(boolean b, String msg, Object... args) throws CsvException {
    if (!b) {
      throw new CsvException(msg, args);
    }
  }

  public class FluentReader implements CsvReader<List<Object>> {
    private final String regexSplitor;
    private List<String> header;
    private Map<Integer, CsvColumn<?>> columnPos;
    private List<CsvColumn<?>> missedColumns;

    public FluentReader() {
      this.regexSplitor = Pattern.quote(splitor);
    }

    @Override
    public Flowable<List<Object>> read(Flowable<String> lines) {
      return lines.map(String::trim)
          .filter(this::filterComment)
          .doOnNext(this::readHeader)
          .skip(1)
          .map(this::parse);
    }

    private boolean filterComment(String line) {
      return !line.startsWith("#");
    }

    private void readHeader(String line) throws CsvException {
      if (header != null) {
        return;
      }
      header = Arrays.asList(line.split(regexSplitor)).stream().map(String::trim).collect(Collectors.toList());
      columnPos = new LinkedHashMap<>();
      for (int i = 0; i < header.size(); i++) {
        String name = header.get(i);
        CsvColumn<?> column = columns.stream().filter(c -> Objects.equals(c.name(), name)).findFirst().orElse(null);
        if (column != null) {
          columnPos.put(i, column);
        }
      }
      columnPos = Collections.unmodifiableMap(columnPos);
      List<CsvColumn<?>> requireds = new ArrayList<>(columns);
      requireds.removeIf(c -> c.optional());
      if (!columnPos.values().containsAll(requireds)) {
        requireds.removeAll(columnPos.values());
        throw new CsvException(
            "Column [" + requireds.stream().map(c -> c.name()).collect(Collectors.joining(", ")) + "] not found.");
      }
      missedColumns = columns.stream()
          .filter(c -> !columnPos.containsValue(c))
          .filter(c -> c.defaultValue() != null)
          .collect(Collectors.toList());
    }

    private List<Object> parse(String line) {
      String[] split = line.split(regexSplitor);
      Object[] result = new Object[columns.size()];
      for (int i = 0; i < result.length; i++) {
        CsvColumn<?> column = columnPos.get(i);
        if (column != null) {
          String str = split.length > i ? split[i].trim() : "";
          Object value;
          if (str.isEmpty()) {
            if (column.defaultValue() == null) {
              continue;
            }
            value = column.defaultValue().get();
          } else {
            value = column.parser().parse(str);
          }
          result[columns.indexOf(column)] = value;
        }
      }
      missedColumns.forEach(c -> result[columns.indexOf(c)] = c.defaultValue().get());
      return Arrays.asList(result);
    }
  }

  private class BeanConstructor<T> implements BeanResultConfig<T> {
    private final Class<T> clz;
    private final List<Method> methods;
    private final List<Field> fields;
    private final Map<CsvColumn<?>, BiConsumer<T, Object>> customHandlers = new HashMap<>();
    private final Map<CsvColumn<?>, ActionE2<T, Object, Exception>> annoHandlers = new HashMap<>();
    private final Map<CsvColumn<?>, String> aliases = new HashMap<>();

    public BeanConstructor(Class<T> clz, UnaryOperator<BeanResultConfig<T>> config) throws CsvException {
      if (uncatch(() -> clz.getDeclaredConstructor()) == null) {
        throw new CsvException("");
      }
      this.clz = clz;
      this.methods = Arrays.asList(ReflectUtil.getAllMethods(clz));
      this.fields = Arrays.asList(ReflectUtil.getAllFields(clz, false));
      config.apply(this);
      prepare();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> BeanResultConfig<T> handle(CsvColumn<E> column, BiConsumer<T, E> setter) {
      customHandlers.put(column, (BiConsumer<T, Object>) setter);
      return this;
    }

    @Override
    public BeanResultConfig<T> alias(CsvColumn<?> column, String propName) {
      aliases.put(column, propName);
      return this;
    }

    @SuppressWarnings("unchecked")
    private <K> void prepare() throws CsvException {
      for (Field f : fields) {
        CSV csv = f.getAnnotation(CSV.class);
        if (csv == null) {
          continue;
        }
        String name = getOrDefault(csv.name(), "", f::getName);
        Class<?> type = PrimitiveTypeUtil.toWrapper(getOrDefault(csv.type(), void.class, f::getType));
        CsvValueParser<K> parser = firstNonNull(
            () -> getOrDefault(csv.parser(), CsvValueParser.class, null).newInstance(),
            () -> CsvValueParser.forType(type))
                .orElseThrow(() -> new CsvException("Can't construct CsvValueParser from %s.", csv));
        assertTrue(type.isAssignableFrom(parser.type()), "CsvValueParser is not matched to the type: %s.", csv);
        String defaultStr = csv.defaultValue();
        Supplier<K> defaultSupplier;
        if (defaultStr.equals("defaultValue")) {
          defaultSupplier = null;
        } else {
          K defaultValue = parser.parse(defaultStr);
          defaultSupplier = () -> defaultValue;
        }
        boolean optional = csv.optional();
        OptionalUtil.ifEmpty(findColumn(name), () -> {
          CsvColumn<?> column = CsvColumn.create(name, parser, defaultSupplier, optional);
          addColumn(column);
          f.setAccessible(true);
          annoHandlers.put(column, (obj, v) -> f.set(obj, v));
        });
      }
      for (Method m : methods) {
        CSV csv = m.getAnnotation(CSV.class);
        if (csv == null) {
          continue;
        }
        assertTrue(m.getParameterCount() == 1, "@CSV method must have 1 parameter. Invalid method: %s", m);
        assertTrue(Modifier.isPublic(m.getModifiers()), "@CSV method must be public. Invalid method: %s", m);
        String name = getOrDefault(csv.name(), "", () -> {
          String n = m.getName();
          if (n.startsWith("set") && n.length() > 3 && Character.isUpperCase(n.charAt(3))) {
            return n.substring(3, 4).toLowerCase() + n.substring(4);
          }
          return n;
        });
        Class<?> type = PrimitiveTypeUtil.toWrapper(getOrDefault(csv.type(), void.class, () -> m.getParameterTypes()[0]));
        CsvValueParser<K> parser = firstNonNull(
            () -> getOrDefault(csv.parser(), CsvValueParser.class, null).newInstance(),
            () -> CsvValueParser.forType(type))
                .orElseThrow(() -> new CsvException("Can't construct CsvValueParser from %s.", csv));
        assertTrue(type.isAssignableFrom(parser.type()), "CsvValueParser is not matched to the type: %s.", csv);
        String defaultStr = csv.defaultValue();
        Supplier<K> defaultSupplier;
        if (defaultStr.equals("defaultValue")) {
          defaultSupplier = null;
        } else {
          K defaultValue = parser.parse(defaultStr);
          defaultSupplier = () -> defaultValue;
        }
        boolean optional = csv.optional();
        OptionalUtil.ifEmpty(findColumn(name), () -> {
          CsvColumn<?> column = CsvColumn.create(name, parser, defaultSupplier, optional);
          addColumn(column);
          annoHandlers.put(column, (obj, v) -> m.invoke(obj, v));
        });
      }
    }

    private <V> V getOrDefault(V value, V useDefault, Supplier<V> def){
      return value.equals(useDefault) ? def.get() : value;
    }

    private T construct(List<Object> line) throws CsvException {
      T obj = uncheck(() -> {
        Constructor<T> con = clz.getDeclaredConstructor();
        con.setAccessible(true);
        return con.newInstance();
      });
      for (int i = 0; i < columns.size(); i++) {
        CsvColumn<?> column = columns.get(i);
        Object value = line.get(i);
        if (injectByCustom(obj, column, value)) {
          debug(format("Set property %s by custom handler.", column.name()));
        } else if (injectByAnno(obj, column, value)) {
          debug(format("Set property %s by annotation.", column.name()));
        } else if (injectBySetter(obj, column, value)) {
          debug(format("Set property %s by setter.", column.name()));
        } else if (injectByField(obj, column, value)) {
          debug(format("Set property %s by field.", column.name()));
        } else {
          throw new CsvException("Can't find property for %s.", column.name());
        }
      }
      return obj;
    }

    private boolean injectByCustom(T obj, CsvColumn<?> column, Object value) {
      BiConsumer<T, Object> handler = customHandlers.get(column);
      if (handler == null) {
        return false;
      }
      try {
        handler.accept(obj, value);
        return true;
      } catch (Exception e) {
        debug("Fail to inject by custom handler.", e);
        return false;
      }
    }

    private boolean injectByAnno(T obj, CsvColumn<?> column, Object value) {
      ActionE2<T, Object, Exception> handler = annoHandlers.get(column);
      if (handler == null) {
        return false;
      }
      try {
        handler.call(obj, value);
        return true;
      } catch (Exception e) {
        debug("Fail to inject by anno.", e);
        return false;
      }
    }

    private boolean injectBySetter(T obj, CsvColumn<?> column, Object value) {
      String setterName = "set" + StringUtil.upperFirst(column.name());
      return methods.stream()
          .filter(m -> m.getName().equals(setterName))
          .filter(m -> m.getParameterCount() == 1)
          .filter(m -> Modifier.isPublic(m.getModifiers()))
          .findFirst()
          .map(m -> {
            try {
              m.invoke(obj, value);
              return true;
            } catch (Exception e) {
              trace().log("Fail to inject by setter.", e);
              return false;
            }
          })
          .orElse(false);
    }

    private boolean injectByField(T obj, CsvColumn<?> column, Object value) {
      return fields.stream()
          .filter(f -> f.getName().equals(column.name()))
          .findFirst()
          .map(f -> {
            try {
              f.setAccessible(true);
              f.set(obj, value);
              return true;
            } catch (Exception e) {
              trace().log("Fail to inject by field.", e);
              return false;
            }
          })
          .orElse(false);
    }
  }
}
