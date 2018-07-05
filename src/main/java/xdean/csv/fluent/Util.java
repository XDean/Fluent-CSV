package xdean.csv.fluent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import xdean.csv.CsvColumn;

class Util {
  static Optional<CsvColumn<?>> findColumn(List<CsvColumn<?>> columns, String name) {
    return columns.stream()
        .filter(c -> Objects.equals(c.name(), name))
        .findAny();
  }
}
