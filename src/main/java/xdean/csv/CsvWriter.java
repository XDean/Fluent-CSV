package xdean.csv;

import java.util.Collection;
import java.util.function.Function;

import io.reactivex.Flowable;

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

  default <R> CsvWriter<R> map(io.reactivex.functions.Function<R, T> func) {
    return lines -> from(lines.map(func));
  }

  @FunctionalInterface
  interface CsvBeanWriter<T> extends CsvWriter<T> {
    default <E> CsvBeanWriter<T> addGetter(CsvColumn<E> column, Function<T, E> getter) {
      return this;
    }

    default <E> CsvBeanWriter<T> addGetter(String column, Function<T, E> getter) {
      return this;
    }
  }
}
