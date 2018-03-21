package xdean.csv;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import lombok.ToString;
import xdean.csv.annotation.CSV;
import xdean.csv.fluent.FluentCSV;

public class Sample {

  @Test
  public void test() throws Exception {
    Path golden = Paths.get(getClass().getClassLoader().getResource("sample.csv").toURI());
    FluentCSV.create()
        .readBean(Person.class)
        .from(golden)
        .forEach(System.out::println);
  }

  @ToString
  static class Person {
    @CSV
    int id;

    @CSV
    String name;

    @CSV(name = "desc", optional = true, defaultValue = "No Description")
    String description;
  }
}
