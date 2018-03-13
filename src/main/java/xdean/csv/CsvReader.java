package xdean.csv;

import java.io.InputStream;

import io.reactivex.Flowable;

public interface CsvReader {
  CsvReader read(InputStream stream);

  CsvReader column(String name, CsvValueParser<?> parser);

  <T> Flowable<T> read(CsvRowParser<T> parser);
}
