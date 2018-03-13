package xdean.csv;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class CsvReaderTest {
  CsvReader reader;
  Path golden;

  @Test
  public void test() throws Exception {
    reader.splitor(",")
        .addColumn(CsvColumn.create("id", CsvValueParser.forInt()))
        .addColumn(CsvColumn.create("name", CsvValueParser.forString()))
        .read(Files.newInputStream(golden))
        .as(Person.class);
  }

  static class Person {
    int id;
    String name;
  }
}
