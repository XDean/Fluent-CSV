package xdean.csv.fluent;

import static xdean.jex.util.function.FunctionAdapter.function;
import static xdean.jex.util.lang.ExceptionUtil.uncatch;
import static xdean.jex.util.lang.ExceptionUtil.uncheck;
import static xdean.jex.util.lang.ExceptionUtil.wrapException;

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
import xdean.jex.extra.function.ActionE2;
import xdean.jex.util.reflect.ReflectUtil;

public class FluentReader implements CsvReader {
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
      BeanConstructor<T> con = new BeanConstructor<>(bean);
      return asList().map(s -> con.construct(s));
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
      private final boolean hasEmptyConstructor;

      public BeanConstructor(Class<T> clz) {
        this.clz = clz;
        this.hasEmptyConstructor = uncatch(() -> clz.getConstructor()) != null;
      }

      private T construct(List<Object> line) throws CsvException {
        if (hasEmptyConstructor) {
          return constructByField(line);
        } else {
          return constructByConstructor(line);
        }
      }

      private T constructByField(List<Object> line) throws CsvException {
        T obj = uncheck(() -> {
          Constructor<T> con = clz.getConstructor();
          con.setAccessible(true);
          return con.newInstance();
        });
        List<Method> methods = Arrays.asList(ReflectUtil.getAllMethods(clz));
        List<Field> fields = Arrays.asList(ReflectUtil.getAllFields(clz, false));
        for (int i = 0; i < columns.size(); i++) {
          CsvColumn<?> column = columns.get(i);
          Object value = line.get(i);
          String setter = "set" + column.name();
          wrapException(CsvException::new, () -> methods.stream()
              .filter(m -> m.getName().toLowerCase().equals(setter))
              .filter(m -> m.getParameterCount() == 1)
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .findFirst()
              .<ActionE2<Object, Object, ?>> map(m -> m::invoke)
              .orElseGet(() -> uncheck(() -> fields.stream()
                  .filter(f -> f.getName().equals(column.name()))
                  .findFirst()
                  .map(function(f -> f.setAccessible(true)))
                  .<ActionE2<Object, Object, ?>> map(f -> f::set)
                  .orElseThrow(() -> new IllegalArgumentException("No such property, " + clz.getName() + "." + column.name()))))
              .call(obj, value));
        }
        return obj;
      }

      private T constructByConstructor(List<Object> line) {
        return null;
      }
    }
  }
}
