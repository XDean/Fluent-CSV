package xdean.csv;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public interface CsvConfig {

  CsvConfig splitor(String splitor) throws IllegalArgumentException;

  CsvConfig addColumn(CsvColumn<?> column) throws IllegalArgumentException;

  default CsvConfig addColumns(CsvColumn<?>... columns) {
    Arrays.stream(columns).forEach(this::addColumn);
    return this;
  }

  List<CsvColumn<?>> columns();

  /********************************** Read ************************************/
  CsvReader<List<Object>> readList();

  default CsvReader<Map<CsvColumn<?>, Object>> readMap() {
    List<CsvColumn<?>> columns = columns();
    return readList().map(l -> {
      Map<CsvColumn<?>, Object> map = new LinkedHashMap<>();
      for (int i = 0; i < columns.size(); i++) {
        map.put(columns.get(i), l.get(i));
      }
      return map;
    });
  }

  default <T> CsvReader<T> readBean(Class<T> bean) {
    return readBean(bean, c -> c);
  }

  <T> CsvReader<T> readBean(Class<T> bean, UnaryOperator<BeanReadConfig<T>> config);

  interface BeanReadConfig<T> {
    <E> BeanReadConfig<T> addSetter(CsvColumn<E> column, BiConsumer<T, E> setter);

    <E> BeanReadConfig<T> addSetter(String column, BiConsumer<T, E> setter);
  }

  /********************************** Write ************************************/
  CsvWriter<List<Object>> writeList();

  default CsvWriter<Map<CsvColumn<?>, Object>> writeMap() {
    List<CsvColumn<?>> columns = columns();
    return writeList().<Map<CsvColumn<?>, Object>> map(
        m -> m.values().stream().sorted(Comparator.comparing(columns::indexOf)).collect(Collectors.toList()));
  }

  default <T> CsvWriter<T> writeBean(Class<T> bean) {
    return writeBean(bean, c -> c);
  }

  <T> CsvWriter<T> writeBean(Class<T> bean, UnaryOperator<BeanWriteConfig<T>> config);

  interface BeanWriteConfig<T> {
    <E> BeanWriteConfig<T> addGetter(CsvColumn<E> column, Function<T, E> getter);

    <E> BeanWriteConfig<T> addGetter(String column, Function<T, E> getter);
  }
}
