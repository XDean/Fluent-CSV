package xdean.csv;

import java.util.List;

import io.reactivex.Flowable;

public interface CsvResult {
  List<CsvColumn<?>> columns();

  <T> Flowable<T> as(Class<?> bean);
}
