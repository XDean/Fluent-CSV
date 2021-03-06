package xdean.csv.fluent;

import static java.lang.String.format;
import static xdean.csv.fluent.Util.findColumn;
import static xdean.jex.util.lang.PrimitiveTypeUtil.toWrapper;
import static xdean.jex.util.task.TaskUtil.firstNonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotationUtils;

import io.reactivex.Flowable;
import xdean.csv.CsvColumn;
import xdean.csv.CsvException;
import xdean.csv.CsvReader;
import xdean.csv.CsvValueParser;
import xdean.csv.annotation.CSV;
import xdean.jex.extra.function.ActionE2;
import xdean.jex.log.Logable;
import xdean.jex.util.reflect.AnnotationUtil;
import xdean.jex.util.reflect.ReflectUtil;
import xdean.jex.util.string.StringUtil;

public class FluentReader implements CsvReader<Map<CsvColumn<?>, Object>>, Logable {

  private static final CSV DEFAULT_CSV_ANNO = AnnotationUtil.createAnnotationFromMap(CSV.class, Collections.emptyMap());
  private final List<CsvColumn<?>> columns;
  private final Configuration config;
  private List<String> header;
  private Map<Integer, CsvColumn<?>> columnPos;
  private List<CsvColumn<?>> missedColumns;

  public FluentReader(FluentCSV fluentCsv) {
    this.columns = new ArrayList<>(fluentCsv.columns);
    this.config = fluentCsv.configuration.build();
  }

  @Override
  public Flowable<Map<CsvColumn<?>, Object>> from(Flowable<String> lines) {
    return lines
        .filter(this::filterComment)
        .doOnNext(this::readHeader)
        .skip(1)
        .map(this::parse);
  }

  public <T> CsvBeanReader<T> asBean(Class<T> bean) throws CsvException {
    return new BeanConstructor<>(bean);
  }

  private boolean addColumn(CsvColumn<?> column) {
    if (findColumn(columns, column.name()).isPresent()) {
      debug("Column " + column.name() + " already exists.");
      return false;
    } else {
      return columns.add(column);
    }
  }

  private boolean filterComment(String line) {
    return !line.startsWith("#");
  }

  private void readHeader(String line) throws CsvException {
    if (header != null) {
      return;
    }
    synchronized (this) {
      if (header != null) {
        return;
      }
      header = config.split(line);
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
  }

  private Map<CsvColumn<?>, Object> parse(String line) throws CsvException {
    List<String> split = config.split(line);
    Map<CsvColumn<?>, Object> result = new HashMap<>();
    for (int i = 0; i < columns.size(); i++) {
      CsvColumn<?> column = columnPos.get(i);
      if (column != null) {
        String str = split.size() > i ? split.get(i) : "";
        Object value;
        if (str.isEmpty()) {
          if (column.defaultValue() == null) {
            if (column.parser().type() == String.class) {
              value = str;
            } else {
              continue;
            }
          } else {
            value = column.defaultValue().get();
          }
        } else {
          value = column.parser().parse(str);
        }
        result.put(column, value);
      }
    }
    missedColumns.forEach(c -> result.put(c, c.defaultValue().get()));
    return Collections.unmodifiableMap(result);
  }

  @SuppressWarnings("unchecked")
  private class BeanConstructor<T> implements CsvBeanReader<T> {
    private final Class<T> clz;
    private final Constructor<T> constructor;
    private final List<Method> methods;
    private final List<Field> fields;
    private final List<CsvColumn<?>> parameters = new ArrayList<>(1);
    private final Map<CsvColumn<?>, BiConsumer<T, Object>> customSetter = new HashMap<>();
    private final Map<CsvColumn<?>, ActionE2<T, Object, Exception>> annoSetter = new HashMap<>();

    public BeanConstructor(Class<T> clz) throws CsvException {
      this.clz = clz;
      this.methods = Arrays.asList(ReflectUtil.getAllMethods(clz));
      this.fields = Arrays.asList(ReflectUtil.getAllFields(clz, false));
      this.constructor = getConstructor();
      prepare();
    }

    private Constructor<T> getConstructor() throws CsvException {
      Constructor<?> result = null;
      Constructor<?>[] constructors = clz.getDeclaredConstructors();
      for (Constructor<?> c : constructors) {
        if (c.isAnnotationPresent(CSV.class)) {
          if (result != null) {
            throw new CsvException("There should be and only be one constructor with @CSV in " + clz);
          } else {
            result = c;
          }
        }
      }
      if (result == null) {
        try {
          result = clz.getDeclaredConstructor();
        } catch (NoSuchMethodException | SecurityException e) {
          throw new CsvException("There is no @CSV constructor nor no-arg constructor in " + clz);
        }
      }
      result.setAccessible(true);
      return (Constructor<T>) result;
    }

    private <K> void prepare() throws CsvException {
      for (Parameter p : constructor.getParameters()) {
        CSV find = AnnotationUtils.getAnnotation(p, CSV.class);
        CSV csv = find == null ? DEFAULT_CSV_ANNO : find;
        String name = getOrDefault(csv, CSV::name, p::getName);
        Class<?> type = toWrapper(getOrDefault(csv, CSV::type, p::getType));
        CsvValueParser<K> parser = firstNonNull(
            () -> getOrDefault(csv, CSV::parser, null).newInstance(),
            () -> CsvValueParser.forType(type))
                .orElseThrow(() -> new CsvException("Can't construct CsvValueParser from %s.", csv));
        CsvException.assertTrue(toWrapper(p.getType()).isAssignableFrom(type), "Type must extends the parameter's type: %s", csv);
        CsvException.assertTrue(type.isAssignableFrom(parser.type()), "CsvValueParser is not matched to the type: %s.", csv);
        String defaultStr = csv.defaultValue();
        Supplier<K> defaultSupplier;
        if (defaultStr.equals(DEFAULT_CSV_ANNO.defaultValue())) {
          defaultSupplier = null;
        } else {
          K defaultValue = parser.parse(defaultStr);
          defaultSupplier = () -> defaultValue;
        }
        boolean optional = csv.optional();
        CsvColumn<?> column = CsvColumn.create(name, parser, defaultSupplier, optional);
        if (addColumn(column)) {
          parameters.add(column);
        }
      }
      for (Field f : fields) {
        CSV csv = AnnotationUtils.getAnnotation(f, CSV.class);
        if (csv == null) {
          continue;
        }
        String name = getOrDefault(csv, CSV::name, f::getName);
        Class<?> type = toWrapper(getOrDefault(csv, CSV::type, f::getType));
        CsvValueParser<K> parser = firstNonNull(
            () -> getOrDefault(csv, CSV::parser, null).newInstance(),
            () -> CsvValueParser.forType(type))
                .orElseThrow(() -> new CsvException("Can't construct CsvValueParser from %s.", csv));
        CsvException.assertTrue(toWrapper(f.getType()).isAssignableFrom(type), "Type must extends the field's type: %s", csv);
        CsvException.assertTrue(type.isAssignableFrom(parser.type()), "CsvValueParser is not matched to the type: %s.", csv);
        String defaultStr = csv.defaultValue();
        Supplier<K> defaultSupplier;
        if (defaultStr.equals(DEFAULT_CSV_ANNO.defaultValue())) {
          defaultSupplier = null;
        } else {
          K defaultValue = parser.parse(defaultStr);
          defaultSupplier = () -> defaultValue;
        }
        boolean optional = csv.optional();
        CsvColumn<?> column = CsvColumn.create(name, parser, defaultSupplier, optional);
        if (addColumn(column)) {
          f.setAccessible(true);
          annoSetter.put(column, (obj, v) -> f.set(obj, v));
        }
      }
      for (Method m : methods) {
        CSV csv = AnnotationUtils.getAnnotation(m, CSV.class);
        if (csv == null || m.getParameterCount() != 1) {
          continue;
        }
        CsvException.assertTrue(Modifier.isPublic(m.getModifiers()), "@CSV method must be public. Invalid method: %s", m);
        String name = getOrDefault(csv, CSV::name, () -> {
          String n = m.getName();
          if (n.startsWith("set") && n.length() > 3 && Character.isUpperCase(n.charAt(3))) {
            return n.substring(3, 4).toLowerCase() + n.substring(4);
          }
          return n;
        });
        Class<?> type = toWrapper(getOrDefault(csv, CSV::type, () -> m.getParameterTypes()[0]));
        CsvValueParser<K> parser = firstNonNull(
            () -> getOrDefault(csv, CSV::parser, null).newInstance(),
            () -> CsvValueParser.forType(type))
                .orElseThrow(() -> new CsvException("Can't construct CsvValueParser from %s.", csv));
        CsvException.assertTrue(toWrapper(m.getParameterTypes()[0]).isAssignableFrom(type),
            "Type must extends the method parameter type: %s", csv);
        CsvException.assertTrue(type.isAssignableFrom(parser.type()), "CsvValueParser is not matched to the type: %s.", csv);
        String defaultStr = csv.defaultValue();
        Supplier<K> defaultSupplier;
        if (defaultStr.equals(DEFAULT_CSV_ANNO.defaultValue())) {
          defaultSupplier = null;
        } else {
          K defaultValue = parser.parse(defaultStr);
          defaultSupplier = () -> defaultValue;
        }
        boolean optional = csv.optional();
        CsvColumn<?> column = CsvColumn.create(name, parser, defaultSupplier, optional);
        if (addColumn(column)) {
          annoSetter.put(column, (obj, v) -> m.invoke(obj, v));
        }
      }
    }

    @Override
    public Flowable<T> from(Flowable<String> lines) {
      return FluentReader.this.from(lines).map(this::construct);
    }

    @Override
    public <E> CsvBeanReader<T> addSetter(CsvColumn<E> column, BiConsumer<T, E> setter) {
      if (columns.contains(column)) {
        customSetter.put(column, (BiConsumer<T, Object>) setter);
      }
      return this;
    }

    @Override
    public <E> CsvBeanReader<T> addSetter(String column, BiConsumer<T, E> setter) {
      findColumn(columns, column).ifPresent(c -> customSetter.put(c, (BiConsumer<T, Object>) setter));
      return this;
    }

    private <V> V getOrDefault(CSV userCsv, Function<CSV, V> attribute, Supplier<V> def) {
      V userValue = attribute.apply(userCsv);
      V defaultValue = attribute.apply(DEFAULT_CSV_ANNO);
      if (Objects.equals(userValue, defaultValue)) {
        return def.get();
      } else {
        return userValue;
      }
    }

    private T construct(Map<CsvColumn<?>, Object> line) throws CsvException {
      List<Object> args = new ArrayList<>(constructor.getParameterCount());
      for (CsvColumn<?> column : parameters) {
        args.add(line.get(column));
      }
      T obj;
      try {
        obj = constructor.newInstance(args.toArray());
      } catch (Exception e) {
        throw new CsvException("Fail to construct " + clz, e);
      }
      for (CsvColumn<?> column : columns) {
        Object value = line.get(column);
        if (value == null || parameters.contains(column)) {
          continue;
        } else if (injectByCustom(obj, column, value)) {
          debug(format("Set property %s by custom setter.", column.name()));
        } else if (injectByAnno(obj, column, value)) {
          debug(format("Set property %s by annotation.", column.name()));
        } else if (injectBySetter(obj, column, value)) {
          debug(format("Set property %s by setter.", column.name()));
        } else if (injectByField(obj, column, value)) {
          debug(format("Set property %s by field.", column.name()));
        } else {
          throw new CsvException("Can't find property for %s.", column);
        }
      }
      return obj;
    }

    private boolean injectByCustom(T obj, CsvColumn<?> column, Object value) {
      BiConsumer<T, Object> setter = customSetter.get(column);
      if (setter == null) {
        return false;
      }
      try {
        setter.accept(obj, value);
        return true;
      } catch (Exception e) {
        debug("Fail to inject by custom handler.", e);
        return false;
      }
    }

    private boolean injectByAnno(T obj, CsvColumn<?> column, Object value) {
      ActionE2<T, Object, Exception> setter = annoSetter.get(column);
      if (setter == null) {
        return false;
      }
      try {
        setter.call(obj, value);
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