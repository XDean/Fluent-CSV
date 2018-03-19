package xdean.csv;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import xdean.csv.fluent.FluentReader;

public class CsvReaderTest {
  CsvReader reader = new FluentReader();

  @Test
  public void test() throws Exception {
    Path golden = Paths.get(getClass().getClassLoader().getResource("person.csv").toURI());
    reader.read(Files.newInputStream(golden))
        .asBean(Person.class)
        .forEach(p -> System.out.println(p));
  }

  public static class Person {
    @CSV
    int id;

    @CSV
    String name;

    @Override
    public String toString() {
      return "Person [id=" + id + ", name=" + name + "]";
    }
  }
}
