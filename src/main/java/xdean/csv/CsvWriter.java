package xdean.csv;

import java.util.Collection;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;

@FunctionalInterface
public interface CsvWriter<T> {
  Flowable<String> from(Flowable<T> data);

  default Flowable<String> from(Collection<T> data) {
    return from(Flowable.fromIterable(data));
  }

  @SuppressWarnings("unchecked")
  default Flowable<String> from(T... data) {
    return from(Flowable.fromArray(data));
  }

  default <R> CsvWriter<R> map(Function<R, T> func) {
    return lines -> from(lines.map(func));
  }
}
