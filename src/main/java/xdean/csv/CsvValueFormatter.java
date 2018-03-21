package xdean.csv;

import java.util.function.Function;

import xdean.jex.util.lang.PrimitiveTypeUtil;

/**
 * CSV value formatter.
 *
 * @apiNote The formatter must be stateless.
 * @author Dean Xu (XDean@github.com)
 * @param <T> the value type.
 */
public interface CsvValueFormatter<T> {
  /**
   * Format the value to text.
   */
  String format(T value);

  /**
   * The value type.
   */
  Class<T> type();

  /**
   * Get default {@link CsvValueFormatter} by using {@link Object#toString()}
   */
  static <T> CsvValueFormatter<T> toString(Class<T> clz) {
    return create(clz, t -> t.toString());
  }

  /**
   * Create {@code CsvValueFormatter<T>} from the {@code Function<T, String>}
   */
  @SuppressWarnings("unchecked")
  static <T> CsvValueFormatter<T> create(Class<T> clz, Function<T, String> function) {
    Class<T> c = (Class<T>) PrimitiveTypeUtil.toWrapper(clz);
    return new CsvValueFormatter<T>() {
      @Override
      public String format(T value) {
        return function.apply(value);
      }

      @Override
      public Class<T> type() {
        return c;
      }
    };
  }
}
