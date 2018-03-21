package xdean.csv.fluent;

import static xdean.csv.fluent.Util.assertTrue;
import static xdean.csv.fluent.Util.findColumn;
import static xdean.jex.util.lang.ExceptionUtil.uncatch;
import static xdean.jex.util.task.TaskUtil.firstNonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import io.reactivex.Flowable;
import xdean.csv.CSV;
import xdean.csv.CsvColumn;
import xdean.csv.CsvConfig.BeanWriteConfig;
import xdean.csv.CsvException;
import xdean.csv.CsvValueParser;
import xdean.csv.CsvWriter;
import xdean.jex.extra.function.FuncE1;
import xdean.jex.log.Logable;
import xdean.jex.util.OptionalUtil;
import xdean.jex.util.lang.PrimitiveTypeUtil;
import xdean.jex.util.reflect.ReflectUtil;

public class FluentWriter implements CsvWriter<List<Object>>, Logable {
  private final List<CsvColumn<?>> columns;
  private final String splitor;

  public FluentWriter(FluentCsv fluentCsv) {
    this.columns = new ArrayList<>(fluentCsv.columns);
    this.splitor = fluentCsv.splitor;
  }

  @Override
  public Flowable<String> from(Flowable<List<Object>> data) {
    return data.map(this::format).startWith(getHeader());
  }

  public <T> CsvWriter<T> asBean(Class<T> bean, UnaryOperator<BeanWriteConfig<T>> config) throws CsvException {
    BeanDeconstructor<T> con = new BeanDeconstructor<>(bean);
    config.apply(con);
    return map(s -> con.deconstruct(s));
  }

  private String getHeader() {
    return columns.stream()
        .map(c -> c.name())
        .collect(Collectors.joining(splitor + " "));
  }

  private String format(List<Object> line) {
    // TODO
    return null;
  }

  private void addColumn(CsvColumn<?> column) {
    if (findColumn(columns, column.name()).isPresent()) {
      debug("Column " + column.name() + " already exists.");
    } else {
      columns.add(column);
    }
  }

  private class BeanDeconstructor<T> implements BeanWriteConfig<T> {
    private final Class<T> clz;
    private final List<Method> methods;
    private final List<Field> fields;
    private final Map<CsvColumn<?>, Function<T, Object>> customGetter = new HashMap<>();
    private final Map<CsvColumn<?>, FuncE1<T, Object, Exception>> annoGetter = new HashMap<>();

    public BeanDeconstructor(Class<T> clz) throws CsvException {
      if (uncatch(() -> clz.getDeclaredConstructor()) == null) {
        throw new CsvException("Bean must declare no-arg constructor.");
      }
      this.clz = clz;
      this.methods = Arrays.asList(ReflectUtil.getAllMethods(clz));
      this.fields = Arrays.asList(ReflectUtil.getAllFields(clz, false));
      prepare();
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
        assertTrue(f.getType().isAssignableFrom(type), "Type must extends the field's type: %s", csv);
        assertTrue(type.isAssignableFrom(parser.type()), "CsvValueParser is not matched to the type: %s.", csv);
        boolean optional = csv.optional();
        OptionalUtil.ifEmpty(findColumn(columns, name), () -> {
          CsvColumn<?> column = CsvColumn.create(name, parser, null, optional);
          addColumn(column);
          f.setAccessible(true);
          annoGetter.put(column, obj -> f.get(obj));
        });
      }
      for (Method m : methods) {
        CSV csv = m.getAnnotation(CSV.class);
        if (csv == null || m.getParameterCount() != 0) {
          continue;
        }
        assertTrue(Modifier.isPublic(m.getModifiers()), "@CSV method must be public. Invalid method: %s", m);
        String name = getOrDefault(csv.name(), "", () -> {
          String n = m.getName();
          if (n.startsWith("set") && n.length() > 3 && Character.isUpperCase(n.charAt(3))) {
            return n.substring(3, 4).toLowerCase() + n.substring(4);
          }
          return n;
        });
        Class<?> type = PrimitiveTypeUtil.toWrapper(getOrDefault(csv.type(), void.class, () -> m.getReturnType()));
        CsvValueParser<K> parser = firstNonNull(
            () -> getOrDefault(csv.parser(), CsvValueParser.class, null).newInstance(),
            () -> CsvValueParser.forType(type))
                .orElseThrow(() -> new CsvException("Can't construct CsvValueParser from %s.", csv));
        assertTrue(m.getReturnType().isAssignableFrom(type), "Type must extends the method parameter type: %s", csv);
        assertTrue(type.isAssignableFrom(parser.type()), "CsvValueParser is not matched to the type: %s.", csv);
        boolean optional = csv.optional();
        OptionalUtil.ifEmpty(findColumn(columns, name), () -> {
          CsvColumn<?> column = CsvColumn.create(name, parser, null, optional);
          addColumn(column);
          annoGetter.put(column, obj -> m.invoke(obj));
        });
      }
    }

    @Override
    public <E> BeanWriteConfig<T> addHandler(CsvColumn<E> column, Function<T, E> getter) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public <E> BeanWriteConfig<T> addHandler(String column, Function<T, E> getter) {
      // TODO Auto-generated method stub
      return null;
    }

    private <V> V getOrDefault(V value, V useDefault, Supplier<V> def) {
      return value.equals(useDefault) ? def.get() : value;
    }

    public List<Object> deconstruct(T s) {
      // TODO Auto-generated method stub
      return null;
    }
  }
}