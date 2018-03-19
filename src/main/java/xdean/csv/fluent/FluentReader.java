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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.reactivex.Flowable;
import xdean.csv.CSV;
import xdean.csv.CsvColumn;
import xdean.csv.CsvException;
import xdean.csv.CsvReader;
import xdean.csv.CsvResult;
import xdean.csv.CsvValueParser;
import xdean.jex.extra.function.ActionE2;
import xdean.jex.extra.function.FuncE0;
import xdean.jex.log.Logable;
import xdean.jex.util.OptionalUtil;
import xdean.jex.util.lang.PrimitiveTypeUtil;
import xdean.jex.util.reflect.ReflectUtil;
import xdean.jex.util.string.StringUtil;

public class FluentReader implements CsvReader, Logable {
  private final List<CsvColumn<?>> columns = new ArrayList<>();
  private String splitor = ",";

  @Override
  public CsvResult read(Flowable<String> lines) {
    return new FluentResult(lines, columns, splitor);
  }

  @Override
  public CsvReader splitor(String splitor) {
    this.splitor = splitor;
    return this;
  }

  @Override
  public CsvReader addColumn(CsvColumn<?> column) {
    findColumn(column.name())
        .ifPresent(c -> Util.throwIt(new IllegalArgumentException("Column " + column.name() + " already exists.")));
    columns.add(column);
    return this;
  }

  @Override
  public CsvReader addColumnsFromBean(Class<?> bean) {

    return this;
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

  public class FluentResult implements CsvResult {
    private Flowable<String> lines;
    private final String regexSplitor;
    private List<String> header;
    private Map<Integer, CsvColumn<?>> columnPos;

    public FluentResult(Flowable<String> lines, List<CsvColumn<?>> columns, String splitor) {
      this.lines = lines.map(String::trim)
          .filter(this::filterComment)
          .doOnNext(this::readHeader)
          .skip(1);
      this.regexSplitor = Pattern.quote(splitor);
    }

    @Override
    public List<CsvColumn<?>> columns() {
      return columns;
    }

    @Override
    public Flowable<List<Object>> asList() {
      return lines.map(this::parse);
    }

    @Override
    public <T> Flowable<T> asBean(Class<T> bean) {
      try {
        BeanConstructor<T> con = new BeanConstructor<>(bean);
        return asList().map(s -> con.construct(s));
      } catch (CsvException e) {
        return Flowable.error(e);
      }
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
        columnPos.put(i, column);
      }
      columnPos = Collections.unmodifiableMap(columnPos);
      if (!columnPos.values().containsAll(columns)) {
        List<CsvColumn<?>> missed = new ArrayList<>(columns);
        missed.removeAll(columnPos.values());
        throw new CsvException(missed.stream().map(c -> c.name()).collect(Collectors.joining(", ")) + " not found.");
      }
    }

    private List<Object> parse(String line) {
      String[] split = line.split(regexSplitor);
      Object[] result = new Object[columns.size()];
      for (int i = 0; i < split.length; i++) {
        CsvColumn<?> column = columnPos.get(i);
        if (column != null) {
          Object value = column.parser().parse(split[i].trim());
          result[columns.indexOf(column)] = value;
        }
      }
      return Arrays.asList(result);
    }

    private class BeanConstructor<T> {
      private final Class<T> clz;
      private final List<Method> methods;
      private final List<Field> fields;
      private final Map<CsvColumn<?>, ActionE2<T, Object, Exception>> annoHandlers = new HashMap<>();

      public BeanConstructor(Class<T> clz) throws CsvException {
        if (uncatch(() -> clz.getDeclaredConstructor()) == null) {
          throw new CsvException("");
        }
        this.clz = clz;
        this.methods = Arrays.asList(ReflectUtil.getAllMethods(clz));
        this.fields = Arrays.asList(ReflectUtil.getAllFields(clz, false));
        prepare();
      }

      private void prepare() throws CsvException {
        for (Field f : fields) {
          CSV csv = f.getAnnotation(CSV.class);
          if (csv == null) {
            continue;
          }
          String name = getOrDefault(csv.name(), "", f::getName);
          Class<?> type = PrimitiveTypeUtil.toWrapper(getOrDefault(csv.type(), void.class, f::getType));
          CsvValueParser<?> parser = firstNonNull(
              () -> getOrDefault(csv.parser(), CsvValueParser.class, null).newInstance(),
              () -> CsvValueParser.forType(type))
                  .orElseThrow(() -> new CsvException("Can't construct CsvValueParser from %s.", csv));
          if (!type.isAssignableFrom(parser.type())) {
            throw new CsvException("CsvValueParser is not matched to the type: %s.", csv);
          }
          OptionalUtil.ifEmpty(findColumn(name), () -> {
            CsvColumn<?> column = CsvColumn.create(name, parser);
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
          assertTrue(m.isAccessible(), "@CSV method must be public. Invalid method: %s", m);
          String name = getOrDefault(csv.name(), "", () -> {
            String n = m.getName();
            if (n.startsWith("set") && n.length() > 3 && Character.isUpperCase(n.charAt(3))) {
              return n.substring(3, 4).toLowerCase() + n.substring(4);
            }
            return n;
          });
          Class<?> type = PrimitiveTypeUtil.toWrapper(getOrDefault(csv.type(), void.class, m::getReturnType));
          CsvValueParser<?> parser = firstNonNull(
              () -> getOrDefault(csv.parser(), CsvValueParser.class, null).newInstance(),
              () -> CsvValueParser.forType(type))
                  .orElseThrow(() -> new CsvException("Can't construct CsvValueParser from %s.", csv));
          if (!type.isAssignableFrom(parser.type())) {
            throw new CsvException("CsvValueParser is not matched to the type: %s.", csv);
          }
          OptionalUtil.ifEmpty(findColumn(name), () -> {
            CsvColumn<?> column = CsvColumn.create(name, parser);
            addColumn(column);
            annoHandlers.put(column, (obj, v) -> m.invoke(obj, v));
          });
        }
      }

      private <V, E extends Exception> V getOrDefault(V value, V useDefault, FuncE0<V, E> def) throws E {
        return value.equals(useDefault) ? def.call() : value;
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
          if (injectByAnno(obj, column, value)) {
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

      private boolean injectByAnno(T obj, CsvColumn<?> column, Object value) {
        ActionE2<T, Object, Exception> handler = annoHandlers.get(column);
        if (handler == null) {
          return false;
        }
        try {
          handler.call(obj, value);
          return true;
        } catch (Exception e) {
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
}
