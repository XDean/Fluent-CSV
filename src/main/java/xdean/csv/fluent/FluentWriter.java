package xdean.csv.fluent;

import static xdean.csv.fluent.Util.assertTrue;
import static xdean.csv.fluent.Util.findColumn;
import static xdean.jex.util.lang.ExceptionUtil.uncatch;
import static xdean.jex.util.lang.PrimitiveTypeUtil.toWrapper;
import static xdean.jex.util.task.TaskUtil.firstNonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotationUtils;

import io.reactivex.Flowable;
import xdean.csv.CsvColumn;
import xdean.csv.CsvException;
import xdean.csv.CsvValueFormatter;
import xdean.csv.CsvWriter;
import xdean.csv.annotation.CSV;
import xdean.jex.extra.function.FuncE1;
import xdean.jex.log.Logable;
import xdean.jex.util.reflect.ReflectUtil;
import xdean.jex.util.string.StringUtil;

public class FluentWriter implements CsvWriter<List<Object>>, Logable {
  private final Configuration config;
  private final List<CsvColumn<?>> columns;
  private List<CsvColumn<?>> sortedColumns;

  public FluentWriter(FluentCSV fluentCsv) {
    this.columns = new ArrayList<>(fluentCsv.columns);
    this.sortedColumns = columns;
    this.config = fluentCsv.configuration.build();
  }

  @Override
  public Flowable<String> from(Flowable<List<Object>> data) {
    return data.map(this::format).startWith(getHeader());
  }

  @Override
  public CsvWriter<List<Object>> sort(Comparator<CsvColumn<?>> comparator) {
    sortedColumns = new ArrayList<>(columns);
    sortedColumns.sort(comparator);
    return this;
  }

  public <T> CsvBeanWriter<T> asBean(Class<T> bean) throws CsvException {
    return new BeanDeconstructor<>(bean);
  }

  private String getHeader() {
    return sortedColumns.stream()
        .map(c -> config.escape(c.name()))
        .collect(Collectors.joining(config.splitor + ""));
  }

  @SuppressWarnings("unchecked")
  private String format(List<Object> line) throws CsvException {
    String[] strs = new String[columns.size()];
    Arrays.fill(strs, "");
    for (int i = 0; i < columns.size(); i++) {
      if (line.size() > i) {
        Object value = line.get(i);
        CsvColumn<?> column = columns.get(i);
        CsvValueFormatter<Object> formatter = (CsvValueFormatter<Object>) column.formatter();
        int index = sortedColumns.indexOf(column);
        assertTrue(formatter.type().isInstance(value), "%s is not instance of %s", value, formatter.type());
        strs[index] = config.escape(formatter.format(value));
      }
    }
    return Arrays.stream(strs).collect(Collectors.joining(config.splitor + ""));
  }

  private boolean addColumn(CsvColumn<?> column) {
    if (findColumn(columns, column.name()).isPresent()) {
      debug("Column " + column.name() + " already exists.");
      return false;
    } else {
      return columns.add(column);
    }
  }

  @SuppressWarnings("unchecked")
  private class BeanDeconstructor<T> implements CsvBeanWriter<T> {
    private final List<Method> methods;
    private final List<Field> fields;
    private final Map<CsvColumn<?>, Function<T, Object>> customGetter = new HashMap<>();
    private final Map<CsvColumn<?>, FuncE1<T, Object, Exception>> annoGetter = new HashMap<>();

    public BeanDeconstructor(Class<T> clz) throws CsvException {
      assertTrue(uncatch(() -> clz.getDeclaredConstructor()) != null, "Bean must declare no-arg constructor.");
      this.methods = Arrays.asList(ReflectUtil.getAllMethods(clz));
      this.fields = Arrays.asList(ReflectUtil.getAllFields(clz, false));
      prepare();
    }

    private <K> void prepare() throws CsvException {
      for (Field f : fields) {
        CSV csv = AnnotationUtils.getAnnotation(f, CSV.class);
        if (csv == null) {
          continue;
        }
        String name = getOrDefault(csv.name(), "", f::getName);
        Class<?> type = toWrapper(getOrDefault(csv.type(), void.class, f::getType));
        CsvValueFormatter<K> formatter = firstNonNull(
            () -> getOrDefault(csv.formatter(), CsvValueFormatter.class, null).newInstance(),
            () -> CsvValueFormatter.toString(type))
                .orElseThrow(() -> new CsvException("Can't construct CsvValueParser from %s.", csv));
        assertTrue(toWrapper(f.getType()).isAssignableFrom(type), "Type must extends the field's type: %s", csv);
        assertTrue(type.isAssignableFrom(formatter.type()), "CsvValueFormatter is not matched to the type: %s.", csv);
        CsvColumn<?> column = CsvColumn.create(name, formatter);
        if (addColumn(column)) {
          f.setAccessible(true);
          annoGetter.put(column, obj -> f.get(obj));
        }
      }
      for (Method m : methods) {
        CSV csv = AnnotationUtils.getAnnotation(m, CSV.class);
        if (csv == null || m.getParameterCount() != 0 || m.getReturnType() == void.class) {
          continue;
        }
        assertTrue(Modifier.isPublic(m.getModifiers()), "@CSV method must be public. Invalid method: %s", m);
        String name = getOrDefault(csv.name(), "", () -> {
          String n = m.getName();
          if (n.startsWith("get") && n.length() > 3 && Character.isUpperCase(n.charAt(3))) {
            return n.substring(3, 4).toLowerCase() + n.substring(4);
          } else if (n.startsWith("is") && n.length() > 2 && Character.isUpperCase(n.charAt(2))) {
            return n.substring(2, 3).toLowerCase() + n.substring(3);
          }
          return n;
        });
        Class<?> type = toWrapper(getOrDefault(csv.type(), void.class, () -> m.getReturnType()));
        CsvValueFormatter<K> parser = firstNonNull(
            () -> getOrDefault(csv.formatter(), CsvValueFormatter.class, null).newInstance(),
            () -> CsvValueFormatter.toString(type))
                .orElseThrow(() -> new CsvException("Can't construct CsvValueParser from %s.", csv));
        assertTrue(toWrapper(m.getReturnType()).isAssignableFrom(type), "Type must extends the method parameter type: %s", csv);
        assertTrue(type.isAssignableFrom(parser.type()), "CsvValueFormatter is not matched to the type: %s.", csv);
        CsvColumn<?> column = CsvColumn.create(name, parser);
        if (addColumn(column)) {
          annoGetter.put(column, obj -> m.invoke(obj));
        }
      }
    }

    @Override
    public Flowable<String> from(Flowable<T> data) {
      return FluentWriter.this.mapFrom(this::deconstruct).from(data);
    }

    @Override
    public CsvBeanWriter<T> sort(Comparator<CsvColumn<?>> comparator) {
      FluentWriter.this.sort(comparator);
      return this;
    }

    @Override
    public <E> CsvBeanWriter<T> addGetter(CsvColumn<E> column, Function<T, E> getter) {
      if (columns.contains(column)) {
        customGetter.put(column, (Function<T, Object>) getter);
      }
      return this;
    }

    @Override
    public <E> CsvBeanWriter<T> addGetter(String column, Function<T, E> getter) {
      findColumn(columns, column).ifPresent(c -> customGetter.put(c, (Function<T, Object>) getter));
      return this;
    }

    public List<Object> deconstruct(T obj) throws CsvException {
      List<Object> result = new ArrayList<>(columns.size());
      for (CsvColumn<?> column : columns) {
        Object value;
        if ((value = getByCustom(obj, column)) != null) {
          debug(String.format("Get property %s by custom getter.", column.name()));
        } else if ((value = getByAnno(obj, column)) != null) {
          debug(String.format("Get property %s by annotation.", column.name()));
        } else if ((value = getByGetter(obj, column)) != null) {
          debug(String.format("Get property %s by getter.", column.name()));
        } else if ((value = getByField(obj, column)) != null) {
          debug(String.format("Get property %s by field.", column.name()));
        } else {
          throw new CsvException("Can't find property for %s.", column);
        }
        result.add(value);
      }
      return result;
    }

    private Object getByCustom(T obj, CsvColumn<?> column) {
      Function<T, Object> getter = customGetter.get(column);
      if (getter == null) {
        return null;
      }
      try {
        return getter.apply(obj);
      } catch (Exception e) {
        debug("Fail to inject by anno.", e);
        return null;
      }
    }

    private Object getByAnno(T obj, CsvColumn<?> column) {
      FuncE1<T, Object, Exception> getter = annoGetter.get(column);
      if (getter == null) {
        return null;
      }
      try {
        return getter.call(obj);
      } catch (Exception e) {
        debug("Fail to inject by anno.", e);
        return null;
      }
    }

    private Object getByGetter(T obj, CsvColumn<?> column) {
      String getName = "get" + StringUtil.upperFirst(column.name());
      String isName = "is" + StringUtil.upperFirst(column.name());
      return methods.stream()
          .filter(
              m -> m.getName().equals(getName) || (toWrapper(m.getReturnType()) == Boolean.class && m.getName().equals(isName)))
          .filter(m -> m.getParameterCount() == 0)
          .filter(m -> m.getReturnType() != void.class)
          .filter(m -> Modifier.isPublic(m.getModifiers()))
          .findFirst()
          .map(m -> {
            try {
              return m.invoke(obj);
            } catch (Exception e) {
              trace().log("Fail to inject by setter.", e);
              return null;
            }
          })
          .orElse(null);
    }

    private Object getByField(T obj, CsvColumn<?> column) {
      return fields.stream()
          .filter(f -> f.getName().equals(column.name()))
          .findFirst()
          .map(f -> {
            try {
              f.setAccessible(true);
              return f.get(obj);
            } catch (Exception e) {
              trace().log("Fail to inject by field.", e);
              return null;
            }
          })
          .orElse(null);
    }

    private <V> V getOrDefault(V value, V useDefault, Supplier<V> def) {
      return value.equals(useDefault) ? def.get() : value;
    }
  }
}