package xdean.csv;

import java.util.List;

public interface CsvRow {
  List<String> getHeader();

  List<String> getData();
}
