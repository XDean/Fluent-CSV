package xdean.csv;

public interface CsvRowParser<T> {
  T parse(CsvRow row);
}
