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

@FunctionalInterface
public interface CsvReader<T> {

  Flowable<T> from(Flowable<String> lines);

  default <R> CsvReader<R> map(Function<T, R> func) {
    return lines -> from(lines).map(func);
  }

  default Flowable<T> from(String input) {
    return from(Flowable.fromArray(input.split("\\R")));
  }

  default Flowable<T> from(Path path) throws IOException {
    return from(Files.newInputStream(path));
  }

  default Flowable<T> from(InputStream stream) {
    return from(new InputStreamReader(stream));
  }

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

  @FunctionalInterface
  interface CsvBeanReader<T> extends CsvReader<T> {
    default <E> CsvBeanReader<T> addSetter(CsvColumn<E> column, BiConsumer<T, E> setter) {
      return this;
    }

    default <E> CsvBeanReader<T> addSetter(String column, BiConsumer<T, E> setter) {
      return this;
    }
  }
}
