package xdean.csv;

import static org.junit.Assert.fail;
import static xdean.csv.CsvColumn.create;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
        .readBean(Person.class)
        .from(golden)
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
        .readBean(A.class)
        .from("a:b\n1:2\n:3\n4:")
        .test()
        .assertNoErrors()
        .assertValueCount(3)
        .assertValues(
            new A(1, 2f, 0),
            new A(0, 3f, 0),
            new A(4, 0f, 0));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddColumnAsMap() throws Exception {
    B.A.toString();
    reader.addColumns(B.A, B.B)
        .readMap()
        .from("a,b\n1,2\n3,4")
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
    class TNC {
    }
    reader.readBean(TNC.class)
        .from("")
        .test()
        .assertError(CsvException.class)
        .assertErrorMessage("Bean must declare no-arg constructor.");
  }

  @Test
  public void testMissColumn() throws Exception {
    reader
        .readBean(A.class)
        .from("a\n1\n3\n4")
        .test()
        .assertError(CsvException.class)
        .assertErrorMessage("Column [b] not found.");
  }

  @Test
  public void testOptional() throws Exception {
    reader
        .readBean(C.class)
        .from("a\n1\n3\n4")
        .test()
        .assertNoErrors()
        .assertValueCount(3)
        .assertValues(
            new C(1, 3.14f, 0),
            new C(3, 3.14f, 0),
            new C(4, 3.14f, 0));
  }

  @Test
  public void testCustomHandler() throws Exception {
    reader
        .addColumn(A.C)
        .readBean(A.class, b -> b
            .<Integer> addHandler("a", (o, v) -> o.setAPlus5(v))
            .addHandler(A.C, (o, v) -> o.c = v + 100))
        .from("a,b,c\n1,2,-1\n3,4,-2")
        .test()
        .assertNoErrors()
        .assertValueCount(2)
        .assertValues(
            new A(6, 2f, 99),
            new A(8, 4f, 98));
  }

  @Test
  public void testWrongParser() throws Exception {
    reader.readBean(TWP.class)
        .from("")
        .test()
        .assertError(CsvException.class)
        .assertError(t -> t.getMessage().startsWith("Can't construct CsvValueParser from"));
  }

  @Test
  public void testSetter() throws Exception {
    reader
        .addColumns(B.A, B.B)
        .readBean(B.class)
        .from("a,b\n1,2\n3,4")
        .test()
        .assertValueCount(2)
        .assertValues(
            new B(1, 2f),
            new B(3, 4f));
  }

  @Test
  public void testCsvName() throws Exception {
    reader.readBean(D.class)
        .from("a,b\n1\n2,3")
        .test()
        .assertValueCount(2)
        .assertValues(
            new D(1, 100),
            new D(2, 3));
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
    public static final CsvColumn<Integer> C = create("c", CsvValueParser.INT);
    @CSV
    int a;
    @CSV
    float b;

    int c;

    public void setAPlus5(int a) {
      this.a = a + 5;
    }
  }

  @EqualsAndHashCode
  @AllArgsConstructor
  @NoArgsConstructor
  public static class B {
    public static final CsvColumn<Integer> A = create("a", CsvValueParser.INT);
    public static final CsvColumn<Float> B = create("b", CsvValueParser.FLOAT);

    public static Map<CsvColumn<?>, Object> asMap(int a, float b) {
      return ImmutableMap.of(
          A, a,
          B, b);
    }

    int a;
    float b;

    public void setA(int a) {
      this.a = a;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class C {
    @CSV
    int a;
    @CSV(optional = true, defaultValue = "3.14")
    float b;
    @CSV(optional = true)
    int c;
  }

  @EqualsAndHashCode
  @NoArgsConstructor
  @AllArgsConstructor
  static class D {
    int a;
    int b;

    @CSV
    public void setA(int a) {
      this.a = a;
    }

    @CSV(defaultValue = "100")
    public void b(int b) {
      this.b = b;
    }
  }

  static class TWP {
    @CSV
    TWP twp;
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
