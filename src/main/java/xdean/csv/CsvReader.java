package xdean.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;

/**
 * CSV reader.
 *
 * @author Dean Xu (XDean@github.com)
 *
 * @param <T> the output type
 */
@FunctionalInterface
public interface CsvReader<T> {

  /**
   * Read from lines.
   */
  Flowable<T> from(Flowable<String> lines);

  /**
   * Map {@code CsvReader<T>} to {@code CsvReader<R>} by {@code Function<T, R>}.
   *
   * @param func the transform function
   * @return the {@code CsvReader<R>}
   */
  default <R> CsvReader<R> map(Function<T, R> func) {
    return lines -> from(lines).map(func);
  }

  /**
   * Read from entire text.
   */
  default Flowable<T> from(String input) {
    return from(Flowable.fromArray(input.split("\\R")));
  }

  /**
   * Read from file.
   */
  default Flowable<T> from(Path path) throws IOException {
    return from(Files.newInputStream(path));
  }

  /**
   * Read from {@link InputStream}.
   */
  default Flowable<T> from(InputStream stream) {
    return from(new InputStreamReader(stream));
  }

  /**
   * Read from {@link Reader}.
   */
  default Flowable<T> from(Reader stream) {
    BufferedReader reader = new BufferedReader(stream);
    return from(Flowable.generate(e -> {
      String line = reader.readLine();
      if (line == null) {
        e.onComplete();
      } else {
        e.onNext(line);
      }
    }));
  }

  /**
   * Bean related configuration.
   */
  @FunctionalInterface
  interface CsvBeanReader<T> extends CsvReader<T> {
    /**
     * Add custom setter for the column. It will overwrite original setter.
     */
    default <E> CsvBeanReader<T> addSetter(CsvColumn<E> column, BiConsumer<T, E> setter) {
      return this;
    }

    /**
     * Add custom setter for the named column. It will overwrite original setter.
     */
    default <E> CsvBeanReader<T> addSetter(String column, BiConsumer<T, E> setter) {
      return this;
    }
  }
}
