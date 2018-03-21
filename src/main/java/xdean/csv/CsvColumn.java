package xdean.csv;

import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;

public interface CsvColumn<T> {
  String name();

  @Nullable
  CsvValueParser<T> parser();

  @Nullable
  CsvValueFormatter<T> formatter();

  @Nullable
  default Supplier<T> defaultValue() {
    return null;
  }

  default boolean optional() {
    return false;
  }

  static <T> CsvColumn<T> create(String name, CsvValueParser<T> parser) {
    return create(name, parser, null);
  }

  static <T> CsvColumn<T> create(String name, CsvValueParser<T> parser, Supplier<T> defaultValue) {
    return create(name, parser, defaultValue, false);
  }

  static <T> CsvColumn<T> create(String name, CsvValueParser<T> parser, Supplier<T> defaultValue, boolean optional) {
    return create(name, parser, null, defaultValue, optional);
  }

  static <T> CsvColumn<T> create(String name, CsvValueFormatter<T> parser) {
    return create(name, parser, null);
  }

  static <T> CsvColumn<T> create(String name, CsvValueFormatter<T> parser, Supplier<T> defaultValue) {
    return create(name, parser, defaultValue, false);
  }

  static <T> CsvColumn<T> create(String name, CsvValueFormatter<T> formatter, Supplier<T> defaultValue, boolean optional) {
    return create(name, null, formatter, defaultValue, optional);
  }

  static <T> CsvColumn<T> create(String name, CsvValueParser<T> parser, CsvValueFormatter<T> formatter, Supplier<T> defaultValue,
      boolean optional) {
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
