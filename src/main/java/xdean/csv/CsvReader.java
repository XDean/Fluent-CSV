package xdean.csv;

import java.io.InputStream;

public interface CsvReader {

  CsvResult read(InputStream stream);

  CsvReader splitor(String splitor);

  CsvReader addColumn(CsvColumn<?> column);

}
