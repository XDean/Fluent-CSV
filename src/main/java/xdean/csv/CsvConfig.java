package xdean.csv;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import xdean.csv.CsvReader.CsvBeanReader;
import xdean.csv.CsvWriter.CsvBeanWriter;

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

  <T> CsvBeanReader<T> readBean(Class<T> bean);

  /********************************** Write ************************************/
  CsvWriter<List<Object>> writeList();

  default CsvWriter<Map<CsvColumn<?>, Object>> writeMap() {
    List<CsvColumn<?>> columns = columns();
    return writeList().<Map<CsvColumn<?>, Object>> map(
        m -> m.values().stream().sorted(Comparator.comparing(columns::indexOf)).collect(Collectors.toList()));
  }

  <T> CsvBeanWriter<T> writeBean(Class<T> bean);
}
