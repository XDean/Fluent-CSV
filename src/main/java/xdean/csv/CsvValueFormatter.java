package xdean.csv;

import java.util.function.Function;

import xdean.jex.util.lang.PrimitiveTypeUtil;

public interface CsvValueFormatter<T> {
  String format(T value) throws RuntimeException;

  Class<T> type();

  static <T> CsvValueFormatter<T> toString(Class<T> clz) {
    return create(clz, t -> t.toString());
  }

  @SuppressWarnings("unchecked")
  static <T> CsvValueFormatter<T> create(Class<T> clz, Function<T, String> function) {
    Class<T> c = (Class<T>) PrimitiveTypeUtil.toWrapper(clz);
    return new CsvValueFormatter<T>() {
      @Override
      public String format(T value) throws RuntimeException {
        return function.apply(value);
      }

      @Override
      public Class<T> type() {
        return c;
      }
    };
  }
}
