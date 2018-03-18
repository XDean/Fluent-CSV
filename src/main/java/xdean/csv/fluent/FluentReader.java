package xdean.csv.fluent;

import static xdean.jex.util.lang.ExceptionUtil.uncatch;
import static xdean.jex.util.lang.ExceptionUtil.uncheck;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.reactivex.Flowable;
import xdean.csv.CsvColumn;
import xdean.csv.CsvException;
import xdean.csv.CsvReader;
import xdean.csv.CsvResult;
import xdean.jex.util.log.Logable;
import xdean.jex.util.reflect.ReflectUtil;

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
    columns.stream()
        .filter(c -> Objects.equals(c.name(), column.name()))
        .findAny()
        .ifPresent(c -> Util.throwIt(new IllegalArgumentException("Column " + column.name() + " already exists.")));
    columns.add(column);
    return this;
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

      public BeanConstructor(Class<T> clz) throws CsvException {
        if (uncatch(() -> clz.getDeclaredConstructor()) == null) {
          throw new CsvException("");
        }
        this.clz = clz;
        this.methods = Arrays.asList(ReflectUtil.getAllMethods(clz));
        this.fields = Arrays.asList(ReflectUtil.getAllFields(clz, false));
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
          if (injectByAnno()) {
          } else if (injectBySetter(obj, column, value)) {
            trace().log("Set property '{0}' by setter.", column.name());
          } else if (injectByField(obj, column, value)) {
            trace().log("Set property '{0}' by field.", column.name());
          } else {
            throw new CsvException("Can't find property for %s.", column.name());
          }
        }
        return obj;
      }

      private boolean injectByAnno() {
        return false;
      }

      private boolean injectBySetter(T obj, CsvColumn<?> column, Object value) {
        String setter = "set" + column.name();
        return methods.stream()
            .filter(m -> m.getName().toLowerCase().equals(setter))
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
