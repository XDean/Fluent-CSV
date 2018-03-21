package xdean.csv;

import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;

import xdean.csv.annotation.OnlyForRead;
import xdean.csv.annotation.OnlyForWrite;

/**
 * CSV Column.
 *
 * @author Dean Xu (XDean@github.com)
 *
 * @param <T> The column type
 */
public interface CsvColumn<T> {
  /**
   * The column name.
   */
  String name();

  /**
   * The column parser.
   */
  @OnlyForRead
  CsvValueParser<T> parser();

  /**
   * The column formatter.
   */
  @OnlyForWrite
  CsvValueFormatter<T> formatter();

  /**
   * If optional, the column's absence will not lead error.
   */
  @OnlyForRead
  default boolean optional() {
    return false;
  }

  /**
   * The column default value factory. It will be used when the value is empty or this is a missed
   * optional column.
   */
  @OnlyForRead(canBeNull = true)
  Supplier<T> defaultValue();

  /**
   * {@link #create(String, CsvValueParser, CsvValueFormatter, Supplier, boolean)}
   */
  static <T> CsvColumn<T> create(String name, CsvValueParser<T> parser) {
    return create(name, parser, null);
  }

  /**
   * {@link #create(String, CsvValueParser, CsvValueFormatter, Supplier, boolean)}
   */
  static <T> CsvColumn<T> create(String name, CsvValueParser<T> parser, Supplier<T> defaultValue) {
    return create(name, parser, defaultValue, false);
  }

  /**
   * {@link #create(String, CsvValueParser, CsvValueFormatter, Supplier, boolean)}
   */
  static <T> CsvColumn<T> create(String name, CsvValueParser<T> parser, Supplier<T> defaultValue, boolean optional) {
    return create(name, parser, null, defaultValue, optional);
  }

  /**
   * {@link #create(String, CsvValueParser, CsvValueFormatter, Supplier, boolean)}
   */
  static <T> CsvColumn<T> create(String name, CsvValueFormatter<T> parser) {
    return create(name, parser, null);
  }

  /**
   * {@link #create(String, CsvValueParser, CsvValueFormatter, Supplier, boolean)}
   */
  static <T> CsvColumn<T> create(String name, CsvValueFormatter<T> parser, Supplier<T> defaultValue) {
    return create(name, parser, defaultValue, false);
  }

  /**
   * {@link #create(String, CsvValueParser, CsvValueFormatter, Supplier, boolean)}
   */
  static <T> CsvColumn<T> create(String name, CsvValueFormatter<T> formatter, Supplier<T> defaultValue, boolean optional) {
    return create(name, null, formatter, defaultValue, optional);
  }

  /**
   * Create {@code CsvColumn<T>} from arguments.
   *
   * @param name the name
   * @param parser the parser, can be null only if formatter is not null.
   * @param formatter the formatter, can be null only if parser is not null.
   * @param defaultValue the default value
   * @param optional optional or not
   * @return the column
   */
  static <T> CsvColumn<T> create(String name,
      @Nullable CsvValueParser<T> parser,
      @Nullable CsvValueFormatter<T> formatter,
      @Nullable Supplier<T> defaultValue,
      boolean optional) {
    if (parser == null && formatter == null) {
      throw new NullPointerException();
    }
    return new CsvColumn<T>() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public CsvValueParser<T> parser() {
        return parser;
      }

      @Override
      public CsvValueFormatter<T> formatter() {
        return formatter;
      }

      @Override
      public Supplier<T> defaultValue() {
        return defaultValue;
      }

      @Override
      public boolean optional() {
        return optional;
      }

      @Override
      public int hashCode() {
        return Objects.hash(name());
      }

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof CsvColumn)) {
          return false;
        }
        return Objects.equals(name(), ((CsvColumn<?>) obj).name());
      }

      @Override
      public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("optional", optional)
            .toString();
      }
    };
  }
}
