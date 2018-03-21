package xdean.csv.fluent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import xdean.csv.CsvColumn;
import xdean.csv.CsvException;

class Util {

  static Optional<CsvColumn<?>> findColumn(List<CsvColumn<?>> columns, String name) {
    return columns.stream()
        .filter(c -> Objects.equals(c.name(), name))
        .findAny();
  }

  static void assertTrue(boolean b, String msg, Object... args) throws CsvException {
    if (!b) {
      throw new CsvException(msg, args);
    }
  }
}
