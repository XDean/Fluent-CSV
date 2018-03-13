package xdean.csv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.reactivex.Flowable;

public interface CsvReader {

  CsvResult read(Flowable<String> lines);

  CsvReader splitor(String splitor);

  CsvReader addColumn(CsvColumn<?> column);

  default CsvResult read(InputStream stream) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
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
