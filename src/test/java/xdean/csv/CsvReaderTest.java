package xdean.csv;

import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

import xdean.csv.CsvReaderTest.Person.House;
import xdean.csv.fluent.FluentReader;

public class CsvReaderTest {
  private static final Person dean = new Person(1, "DEAN", 100, House.NO, "");
  private static final Person wenzhe = new Person(2, "WEN-ZHE", 888, House.YES, "");
  private static final Person xian = new Person(3, "XIAN", 998, House.YES, "manager");

  CsvReader reader;
  Path golden;
  CsvColumn<Integer> id = CsvColumn.create("id", CsvValueParser.INT);
  CsvColumn<String> name = CsvColumn.create("name", new UpperParser());
  CsvColumn<Double> money = CsvColumn.create("money", CsvValueParser.DOUBLE);
  CsvColumn<House> house = CsvColumn.create("has_house", CsvValueParser.forEnum(House.class));
  CsvColumn<String> extra = CsvColumn.create("extra", CsvValueParser.STRING, () -> "");

  @Before
  public void setup() throws Exception {
    reader = new FluentReader();
    golden = getGolden("person.csv");
  }

  private Path getGolden(String name) throws URISyntaxException {
    return Paths.get(getClass().getClassLoader().getResource(name).toURI());
  }

  @Test
  public void test() throws Exception {
    reader
        .addColumn(money)
        .read(Files.newInputStream(golden))
        .asBean(Person.class)
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
    golden = getGolden("person_use_space.csv");
    reader.splitor(" ")
        .addColumn(money)
        .read(Files.newInputStream(golden))
        .asBean(Person.class)
        .test()
        .assertNoErrors()
        .assertValueCount(3)
        .assertValues(
            dean,
            wenzhe,
            xian);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAsMap() throws Exception {
    reader
        .addColumns(id, money, name, house, extra)
        .read(Files.newInputStream(golden))
        .asMap()
        .test()
        .assertNoErrors()
        .assertValueCount(3)
        .assertValues(
            asMap(dean),
            asMap(wenzhe),
            asMap(xian));
  }

  private Map<CsvColumn<?>, Object> asMap(Person p) {
    return ImmutableMap.of(
        this.id, p.id,
        this.name, p.name,
        this.money, p.money,
        this.house, p.house,
        this.extra, p.extra);
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

    public Person() {
    }

    public Person(int id, String name, double money, House house, String extra) {
      this.id = id;
      this.name = name;
      this.money = money;
      this.house = house;
      this.extra = extra;
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

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("id", id)
          .add("name", name)
          .add("money", money)
          .add("extra", extra)
          .toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, name, money, extra);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (obj == null) {
        return false;
      } else if (getClass() != obj.getClass()) {
        return false;
      }
      Person other = (Person) obj;
      return Objects.equals(id, other.id) &&
          Objects.equals(money, other.money) &&
          Objects.equals(name, other.name) &&
          Objects.equals(extra, other.extra);
    }
  }
}
