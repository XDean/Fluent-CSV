package xdean.csv;

import static org.junit.Assert.*;
import static xdean.csv.CsvColumn.create;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xdean.csv.CsvReaderTest.Person.House;
import xdean.csv.fluent.FluentCsv;

public class CsvReaderTest {
  private static final Person dean = new Person(1, "DEAN", 100, House.NO, "");
  private static final Person wenzhe = new Person(2, "WEN-ZHE", 888, House.YES, "");
  private static final Person xian = new Person(3, "XIAN", 998, House.YES, "manager");

  CsvConfig reader;
  Path golden;

  @Before
  public void setup() throws Exception {
    reader = new FluentCsv();
    golden = getGolden("person.csv");
  }

  private Path getGolden(String name) throws URISyntaxException {
    return Paths.get(getClass().getClassLoader().getResource(name).toURI());
  }

  @Test
  public void test() throws Exception {
    reader
        .asBean(Person.class)
        .read(golden)
        .doOnError(e -> e.printStackTrace())
        .test()
        .assertNoErrors()
        .assertValueCount(3)
        .assertValues(
            dean,
            wenzhe,
            xian);
  }

  @Test
  public void testSplitor() throws Exception {
    reader.splitor(":")
        .asBean(A.class)
        .read("a:b\n1:2\n:3\n4:")
        .test()
        .assertNoErrors()
        .assertValueCount(3)
        .assertValues(
            new A(1, 2f),
            new A(0, 3f),
            new A(4, 0f));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddColumnAsMap() throws Exception {
    B.A.toString();
    reader.addColumns(B.A, B.B)
        .asMap()
        .read("a,b\n1,2\n3,4")
        .test()
        .assertNoErrors()
        .assertValueCount(2)
        .assertValues(
            B.asMap(1, 2f),
            B.asMap(3, 4f));
  }

  @Test(expected = CsvException.class)
  public void testUnkown() throws Exception {
    CsvValueParser.forType(CsvReaderTest.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDuplicateColumn() throws Exception {
    reader.addColumn(B.A).addColumn(B.A);
  }

  @Test
  public void testNoConstructor() throws Exception {
    reader.asBean(B.class)
        .read("")
        .test()
        .assertError(CsvException.class)
        .assertErrorMessage("Bean must declare no-arg constructor.");
  }

  public static class UpperParser implements CsvValueParser<String> {
    @Override
    public String parse(String value) throws RuntimeException {
      return value.toUpperCase();
    }

    @Override
    public Class<String> type() {
      return String.class;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class A {
    @CSV
    int a;
    @CSV
    float b;
  }

  public static class B {
    public static final CsvColumn<Integer> A = create("a", CsvValueParser.INT);
    public static final CsvColumn<Float> B = create("b", CsvValueParser.FLOAT);

    public static Map<CsvColumn<?>, Object> asMap(int a, float b) {
      return ImmutableMap.of(
          A, a,
          B, b);
    }

    private B(int i) {
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Person {
    public enum House {
      YES,
      NO;
    }

    @CSV
    int id;

    @CSV(parser = UpperParser.class)
    String name;

    @CSV(type = Double.class)
    double money;

    House house;

    @CSV(defaultValue = "")
    String extra;

    @CSV(name = "has_house")
    public void setHouse(House house) {
      this.house = house;
    }

    public void setExtra(String extra) {
      fail();
    }

    public void setExtraSafe(String extra) {
      this.extra = extra;
    }
  }
}
