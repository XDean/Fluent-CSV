package xdean.csv;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.*;
import static xdean.csv.CsvColumn.create;
import static xdean.jex.util.lang.ExceptionUtil.throwIt;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import xdean.csv.CsvReaderTest.Person.House;
import xdean.csv.annotation.CSV;
import xdean.csv.annotation.CsvConfig;
import xdean.csv.fluent.FluentCSV;

public class CsvReaderTest {
  private static final Person dean = new Person(1, "DEAN", 100, House.NO, "", false);
  private static final Person wenzhe = new Person(2, "WEN-ZHE", 888, House.YES, "", false);
  private static final Person xian = new Person(3, "XIAN", 998, House.YES, "manager", false);

  CsvConfiguration reader;
  Path golden;

  @Before
  public void setup() throws Exception {
    reader = FluentCSV.create();
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
    reader.splitor(' ')
        .readBean(A.class)
        .from("a b\n1 2\n 3\n4 ")
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
        .assertError(t -> t.getMessage().contains("There is no @CSV constructor nor no-arg constructo"));
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
        .readBean(A.class)
        .<Integer> addSetter("a", (o, v) -> o.setAPlus5(v))
        .addSetter(A.C, (o, v) -> o.c = v + 100)
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
    reader.readBean(WrongParser.class)
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

  @Test
  public void testGetError() throws Exception {
    reader
        .addColumn(B.A)
        .readBean(E.class)
        .addSetter(B.A, (e, v) -> throwIt(new RuntimeException()))
        .from("a\n1")
        .test()
        .assertError(CsvException.class)
        .assertError(e -> e.getMessage().contains("Can't find property"));
  }

  @Test
  public void testEscape() throws Exception {
    reader.readConfig(F.class)
        .readBean(F.class)
        .from("i/:d:b\n1:'2/'3'\n4: '5:///:6/n'")
        .test()
        .assertNoErrors()
        .assertValueCount(2)
        .assertValues(
            new F(1, "2'3"),
            new F(4, " 5:/:6\n"));
  }

  @Test
  public void testWrongEscape() throws Exception {
    reader
        .readConfig(F.class)
        .readBean(F.class)
        .from("i/:d:b\n1:2/")
        .test()
        .assertError(CsvException.class)
        .assertError(e -> e.getMessage().contains("Can't end with escaper"));
    reader
        .readConfig(F.class)
        .readBean(F.class)
        .from("i/:d:b\n1:'")
        .test()
        .assertError(CsvException.class)
        .assertError(e -> e.getMessage().contains("The quote must close"));
    reader
        .readConfig(F.class)
        .readBean(F.class)
        .from("i/:d:b\n1:2/1")
        .test()
        .assertError(CsvException.class)
        .assertError(e -> e.getMessage().contains("cannot be escaped"));
  }

  @Ignore
  @Test
  public void testWrongMethod() throws Exception {
    reader.readBean(WrongMethod.class)
        .from("")
        .test()
        .assertError(CsvException.class)
        .assertError(e -> e.getMessage().contains("must have only one paramter"));
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

  @EqualsAndHashCode
  static class E {
    @CSV
    public void setA(int i) {
      throw new RuntimeException();
    }
  }

  @CsvConfig(escaper = '/', splitor = ':', quoter = '\'', ignoreLeadingSpace = false)
  @EqualsAndHashCode
  @NoArgsConstructor
  @AllArgsConstructor
  static class F {
    @CSV(name = "i:d")
    int a;
    @CSV
    String b;
  }

  static class WrongParser {
    @CSV
    WrongParser twp;
  }

  static class WrongMethod {
    @CSV
    public void func(int a, int b) {
    }
  }

  @CSV(type = Double.class)
  @Retention(RUNTIME)
  @Target({ FIELD, METHOD })
  @interface DoubleColumn {

  }

  @Data
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

    @DoubleColumn
    double money;

    House house;

    @CSV(defaultValue = "")
    String extra;

    boolean absent;

    @CSV
    public Person(@CSV(optional = true, defaultValue = "false") boolean absent) {
      this.absent = absent;
    }

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
