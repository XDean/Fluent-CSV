package xdean.csv;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

public interface CsvConfig {

  CsvConfig splitor(String splitor) throws IllegalArgumentException;

  CsvConfig addColumn(CsvColumn<?> column) throws IllegalArgumentException;

  default CsvConfig addColumns(CsvColumn<?>... columns) {
    Arrays.stream(columns).forEach(this::addColumn);
    return this;
  }

  List<CsvColumn<?>> columns();

  CsvReader<List<Object>> asList();

  default CsvReader<Map<CsvColumn<?>, Object>> asMap() {
    List<CsvColumn<?>> columns = columns();
    return asList().map(l -> {
      Map<CsvColumn<?>, Object> map = new LinkedHashMap<>();
      for (int i = 0; i < columns.size(); i++) {
        map.put(columns.get(i), l.get(i));
      }
      return map;
    });
  }

  default <T> CsvReader<T> asBean(Class<T> bean) {
    return asBean(bean, c -> c);
  }

  <T> CsvReader<T> asBean(Class<T> bean, UnaryOperator<BeanConfig<T>> config);

  interface BeanConfig<T> {
    <E> BeanConfig<T> addHandler(CsvColumn<E> column, BiConsumer<T, E> setter);

    <E> BeanConfig<T> addHandler(String column, BiConsumer<T, E> setter);
  }
}
