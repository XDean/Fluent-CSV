package xdean.csv;

import java.util.function.Function;

public interface CsvValueFormatter<T> {
  String parse(T value) throws RuntimeException;

  Class<T> type();

  static <T> CsvValueFormatter<T> create(Class<T> clz, Function<T, String> function) {
    return new CsvValueFormatter<T>() {
      @Override
      public String parse(T value) throws RuntimeException {
        return function.apply(value);
      }

      @Override
      public Class<T> type() {
        return clz;
      }
    };
  }
}
