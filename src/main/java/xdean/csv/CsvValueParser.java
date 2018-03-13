package xdean.csv;

import java.util.function.Function;

public interface CsvValueParser<T> {
  T parse(String value) throws RuntimeException;

  Class<T> type();

  static <T> CsvValueParser<T> create(Class<T> clz, Function<String, T> function) {
    return new CsvValueParser<T>() {
      @Override
      public T parse(String value) throws RuntimeException {
        return function.apply(value);
      }

      @Override
      public Class<T> type() {
        return clz;
      }
    };
  }

  static CsvValueParser<Integer> forInt() {
    return create(Integer.class, Integer::valueOf);
  }

  static CsvValueParser<Double> forDouble() {
    return create(Double.class, Double::valueOf);
  }

  static CsvValueParser<String> forString() {
    return create(String.class, v -> v);
  }
}
