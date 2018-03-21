package xdean.csv;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import xdean.jex.util.lang.PrimitiveTypeUtil;

public interface CsvValueParser<T> {
  T parse(String value) throws RuntimeException;

  Class<T> type();

  @SuppressWarnings("unchecked")
  static <T> CsvValueParser<T> create(Class<T> clz, Function<String, T> function) {
    Class<T> c = (Class<T>) PrimitiveTypeUtil.toWrapper(clz);
    return new CsvValueParser<T>() {
      @Override
      public T parse(String value) throws RuntimeException {
        return function.apply(value);
      }

      @Override
      public Class<T> type() {
        return c;
      }
    };
  }

  CsvValueParser<String> STRING = Helper.create(String.class, v -> v);
  CsvValueParser<Integer> INT = Helper.create(Integer.class, Integer::valueOf);
  CsvValueParser<Long> LONG = Helper.create(Long.class, Long::valueOf);
  CsvValueParser<Float> FLOAT = Helper.create(Float.class, Float::valueOf);
  CsvValueParser<Double> DOUBLE = Helper.create(Double.class, Double::valueOf);
  CsvValueParser<Boolean> BOOLEAN = Helper.create(Boolean.class, Boolean::valueOf);

  static class Helper {
    private static final Map<Class<?>, CsvValueParser<?>> DEFAULTS = new HashMap<>();

    private static <T> CsvValueParser<T> create(Class<T> clz, Function<String, T> function) {
      CsvValueParser<T> parser = CsvValueParser.create(clz, function);
      DEFAULTS.put(clz, parser);
      if (PrimitiveTypeUtil.isWrapper(clz)) {
        DEFAULTS.put(PrimitiveTypeUtil.toPrimitive(clz), parser);
      }
      return parser;
    }
  }

  static <T extends Enum<T>> CsvValueParser<T> forEnum(Class<T> clz) {
    return CsvValueParser.create(clz, v -> Enum.valueOf(clz, v));
  }

  @SuppressWarnings("unchecked")
  static <T, K extends Enum<K>> CsvValueParser<? extends T> forType(Class<T> clz) throws CsvException {
    CsvValueParser<?> parser = Helper.DEFAULTS.get(clz);
    if (parser != null) {
      return (CsvValueParser<? extends T>) parser;
    } else if (clz.isEnum()) {
      return (CsvValueParser<? extends T>) forEnum((Class<K>) clz);
    } else {
      throw new CsvException("Unknown type: " + clz);
    }
  }
}
