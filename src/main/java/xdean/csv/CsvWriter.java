package xdean.csv;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;

public interface CsvWriter<T> {
  Flowable<String> from(Flowable<T> data);

  default <R> CsvWriter<R> map(Function<R, T> func) {
    return lines -> from(lines.map(func));
  }
}
