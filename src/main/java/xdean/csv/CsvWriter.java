package xdean.csv;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

import io.reactivex.Flowable;

/**
 * CSV writer.
 *
 * @implNote To keep the interface functional, some methods have a empty default implementation.
 *           Implementations should override them.
 * @author Dean Xu (XDean@github.com)
 * @param <T> the input type
 */
@FunctionalInterface
public interface CsvWriter<T> {
  /**
   * Write from {@link Flowable} data.
   */
  Flowable<String> from(Flowable<T> data);

  /**
   * Write from {@link Collection} data.
   */
  default Flowable<String> from(Collection<T> data) {
    return from(Flowable.fromIterable(data));
  }

  /**
   * Write from array data.
   */
  @SuppressWarnings("unchecked")
  default Flowable<String> from(T... data) {
    return from(Flowable.fromArray(data));
  }

  /**
   * Sort the output columns.
   */
  default CsvWriter<T> sort(Comparator<CsvColumn<?>> comparator) {
    return this;
  }

  /**
   * Map the input R to T.
   */
  default <R> CsvWriter<R> map(io.reactivex.functions.Function<R, T> func) {
    return lines -> from(lines.map(func));
  }

  /**
   * Bean related configuration.
   */
  @FunctionalInterface
  interface CsvBeanWriter<T> extends CsvWriter<T> {
    /**
     * Add custom getter for the column. It will overwrite original getter.
     */
    default <E> CsvBeanWriter<T> addGetter(CsvColumn<E> column, Function<T, E> getter) {
      return this;
    }

    /**
     * Add custom getter for the named column. It will overwrite original getter.
     */
    default <E> CsvBeanWriter<T> addGetter(String column, Function<T, E> getter) {
      return this;
    }

    @Override
    default CsvBeanWriter<T> sort(Comparator<CsvColumn<?>> comparator) {
      return this;
    }
  }
}
