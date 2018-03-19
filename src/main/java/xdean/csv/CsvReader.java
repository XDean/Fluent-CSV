package xdean.csv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

import io.reactivex.Flowable;

public interface CsvReader {

  CsvReader splitor(String splitor);

  CsvReader addColumn(CsvColumn<?> column);

  default CsvReader addColumns(CsvColumn<?>... columns) {
    Arrays.stream(columns).forEach(this::addColumn);
    return this;
  }

  CsvResult read(Flowable<String> lines);

  default CsvResult read(String input) {
    return read(Flowable.fromArray(input.split("\\R")));
  }

  default CsvResult read(InputStream stream) {
    return read(new InputStreamReader(stream));
  }

  default CsvResult read(Reader stream) {
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
