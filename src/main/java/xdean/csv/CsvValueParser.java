package xdean.csv;

import java.text.ParseException;

@FunctionalInterface
public interface CsvValueParser<T> {
  T parse(String value) throws ParseException;

  static CsvValueParser<Integer> forInt() {
    return Integer::valueOf;
  }
}
