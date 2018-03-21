package xdean.csv;

import static xdean.jex.util.lang.ExceptionUtil.throwIt;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import xdean.csv.fluent.FluentCsv;

@SuppressWarnings("unchecked")
public class CsvWriterTest {

  CsvConfig writer;

  @Before
  public void setup() throws Exception {
    writer = new FluentCsv();
  }

  @Test
  public void testBean() throws Exception {
    writer
        .writeBean(A.class)
        .from(new A(1, 2), new A(3, 4))
        .test()
        .assertNoErrors()
        .assertValueCount(3)
        .assertValues("a, b",
            "1, 2",
            "3, 4");
  }

  @Test
  public void testName() throws Exception {
    writer
        .addColumns(A.A, A.B)
        .writeMap()
        .from(
            ImmutableMap.of(A.A, 1, A.B, 2),
            ImmutableMap.of(A.A, 3, A.B, 4))
        .test()
        .assertNoErrors()
        .assertValueCount(3)
        .assertValues("a, b",
            "1, 2",
            "3, 4");
  }

  @Test
  public void testCustom() throws Exception {
    writer
        .addColumn(A.A)
        .writeBean(A.class)
        .addGetter(A.B, a -> a.b + 100)
        .addGetter("a", a -> a.a - 100)
        .from(new A(1, 2), new A(3, 4))
        .test()
        .assertNoErrors()
        .assertValueCount(3)
        .assertValues("a, b",
            "-99, 102",
            "-97, 104");
  }

  @Test
  public void testFormatter() throws Exception {
    writer.writeBean(B.class)
        .from(new B(1.3, 0), new B(2.7, 0))
        .test()
        .assertNoErrors()
        .assertValueCount(3)
        .assertValues("a, constant",
            "1, 100",
            "2, 100");
  }

  @Test
  public void testNoConstructor() throws Exception {
    class TNC {
    }
    writer.writeBean(TNC.class)
        .from()
        .test()
        .assertError(CsvException.class)
        .assertErrorMessage("Bean must declare no-arg constructor.");
  }

  @Test
  public void testLessData() throws Exception {
    writer
        .addColumns(A.A, A.B)
        .writeList()
        .from(Arrays.asList(1), Arrays.asList(2))
        .test()
        .assertNoErrors()
        .assertValueCount(3)
        .assertValues("a, b",
            "1, ",
            "2, ");
  }

  @Test
  public void testWrongFormatter() throws Exception {
    writer.addColumn(A.B)
        .writeList()
        .from(Arrays.asList(new Object()))
        .test()
        .assertError(CsvException.class)
        .assertError(t -> t.getMessage().contains("instance of"));
  }

  @Test
  public void testGetter() throws Exception {
    writer.addColumns(A.A, A.B, C.D)
        .writeBean(C.class)
        .from(new C(1, 2, true, 3), new C(4, 5, false, 6))
        .test()
        .assertNoErrors()
        .assertValueCount(3)
        .assertValues("a, b, d, c",
            "1, 2, 3, true",
            "4, 5, 6, false");
  }

  @Test
  public void testGetError() throws Exception {
    writer
        .writeBean(D.class)
        .addGetter(A.A, o -> throwIt(new RuntimeException()))
        .from(new D())
        .test()
        .assertError(CsvException.class)
        .assertError(t -> t.getMessage().contains("Can't find property"));
  }

  @EqualsAndHashCode
  @NoArgsConstructor
  @AllArgsConstructor
  public static class A {
    static final CsvColumn<Integer> A = CsvColumn.create("a", (CsvValueFormatter<Integer>) null);
    static final CsvColumn<Integer> B = CsvColumn.create("b", CsvValueFormatter.toString(Integer.class));
    @CSV
    int a;
    @CSV
    int b;

    public void func() {
    }

    @CSV
    public void bar(int i) {
    }

    @CSV
    public void foo() {
    }
  }

  @EqualsAndHashCode
  @NoArgsConstructor
  @AllArgsConstructor
  public static class B {

    public static class Formatter implements CsvValueFormatter<Double> {
      @Override
      public String format(Double value) throws RuntimeException {
        return Integer.toString(value.intValue());
      }

      @Override
      public Class<Double> type() {
        return Double.class;
      }
    }

    @CSV(formatter = Formatter.class)
    double a;

    int b;

    @CSV
    public int constant() {
      return 100;
    }
  }

  @EqualsAndHashCode
  @NoArgsConstructor
  @AllArgsConstructor
  public static class C {
    static final CsvColumn<Integer> D = CsvColumn.create("d", CsvValueFormatter.toString(Integer.class));
    int a;
    int b;
    boolean c;
    int d;

    @CSV
    public int getB() {
      return b;
    }

    @CSV
    public boolean isC() {
      return c;
    }
  }

  @EqualsAndHashCode
  public static class D {
    @CSV
    public int getA() {
      throw new RuntimeException();
    }
  }
}
