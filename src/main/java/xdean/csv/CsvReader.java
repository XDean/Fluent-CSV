package xdean.csv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;

@FunctionalInterface
public interface CsvReader<T> {

  Flowable<T> read(Flowable<String> lines);

  default <R> CsvReader<R> map(Function<T, R> func) {
    return lines -> read(lines).map(func);
  }

  default Flowable<T> read(String input) {
    return read(Flowable.fromArray(input.split("\\R")));
  }

  default Flowable<T> read(InputStream stream) {
    return read(new InputStreamReader(stream));
  }

  default Flowable<T> read(Reader stream) {
    BufferedReader reader = new BufferedReader(stream);
    return read(Flowable.generate(e -> {
      String line = reader.readLine();
      if (line == null) {
        e.onComplete();
      } else {
        e.onNext(line);
      }
    }));
  }
}
