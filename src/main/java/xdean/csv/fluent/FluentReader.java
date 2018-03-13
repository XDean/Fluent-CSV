package xdean.csv.fluent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import io.reactivex.Flowable;
import xdean.csv.CsvColumn;
import xdean.csv.CsvReader;
import xdean.csv.CsvResult;

public class FluentReader implements CsvReader {
  private final List<CsvColumn<?>> columns = new ArrayList<>();
  private String splitor = ",";

  @Override
  public CsvResult read(Flowable<String> lines) {
    return new FluentResult(lines, columns, splitor);
  }

  @Override
  public CsvReader splitor(String splitor) {
    this.splitor = splitor;
    return this;
  }

  @Override
  public CsvReader addColumn(CsvColumn<?> column) {
    columns.stream()
        .filter(c -> Objects.equals(c.name(), column.name()))
        .findAny()
        .ifPresent(c -> Util.throwIt(new IllegalArgumentException("Column " + column.name() + " already exists.")));
    columns.add(column);
    return this;
  }

  public class FluentResult implements CsvResult {
    private Flowable<String> lines;
    private final String regexSplitor;

    public FluentResult(Flowable<String> lines, List<CsvColumn<?>> columns, String splitor) {
      this.lines = lines;
      this.regexSplitor = Pattern.quote(splitor);
      lines
          .map(String::trim)
          .filter(this::filterComment)
          .doOnNext(this::readHeader)
          .skip(1);
    }

    @Override
    public List<CsvColumn<?>> columns() {
      return columns;
    }

    @Override
    public Flowable<List<Object>> asList() {
      return lines.map(this::parse);
    }

    @Override
    public <T> Flowable<T> asBean(Class<T> bean) {
      BeanConstructor<T> con = new BeanConstructor<>(bean);
      return lines.map(s -> con.construct(s));
    }

    private boolean filterComment(String line) {
      return line.startsWith("#");
    }

    private void readHeader(String line) {

    }

    private List<Object> parse(String line) {
      String[] split = line.split(regexSplitor);
      return null;
    }

    private class BeanConstructor<T> {
      private final Class<T> clz;

      public BeanConstructor(Class<T> clz) {
        this.clz = clz;
      }

      private T construct(String line) {
        return null;
      }
    }
  }
}
