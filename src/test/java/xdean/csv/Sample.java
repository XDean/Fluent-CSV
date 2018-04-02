package xdean.csv;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xdean.csv.annotation.CSV;
import xdean.csv.annotation.CsvConfig;
import xdean.csv.fluent.FluentCSV;

public class Sample {

  @Test
  public void test() throws Exception {
    Path golden = Paths.get(getClass().getClassLoader().getResource("sample.csv").toURI());
    FluentCSV.create()
        .readConfig(Person.class)
        .readBean(Person.class)
        .from(golden)
        .doOnNext(System.out::println)
        .test()
        .assertValueCount(3)
        .assertValues(
            new Person(1, "Mike", "Football&Basketball"),
            new Person(2, "Dan", "Swim and walk"),
            new Person(3, "Alex", "No Description"));
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @CsvConfig(splitor = ' ', quoter='\'')
  static class Person {
    @CSV
    int id;

    @CSV
    String name;

    @CSV(name = "desc", optional = true, defaultValue = "No Description")
    String description;
  }
}
