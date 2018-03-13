package xdean.csv;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;

public interface CsvResult {
  List<CsvColumn<?>> columns();

  Flowable<List<Object>> asList();

  default Flowable<Map<CsvColumn<?>, Object>> asMap() {
    List<CsvColumn<?>> columns = columns();
    return asList().map(l -> {
      Map<CsvColumn<?>, Object> map = new LinkedHashMap<>();
      for (int i = 0; i < columns.size(); i++) {
        map.put(columns.get(i), l.get(i));
      }
      return map;
    });
  }

  <T> Flowable<T> asBean(Class<T> bean);
}
