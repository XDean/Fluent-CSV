package xdean.csv.fluent;

import static xdean.csv.fluent.Util.findColumn;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import xdean.csv.CsvColumn;
import xdean.csv.CsvConfig;
import xdean.csv.CsvException;
import xdean.csv.CsvReader;
import xdean.csv.CsvReader.CsvBeanReader;
import xdean.csv.CsvWriter;
import xdean.csv.CsvWriter.CsvBeanWriter;
import xdean.jex.log.Logable;

public class FluentCSV implements CsvConfig, Logable {

  public static FluentCSV create() {
    return new FluentCSV();
  }

  final List<CsvColumn<?>> columns = new ArrayList<>();
  String splitor = ",";

  private FluentCSV() {
  }

  @Override
  public List<CsvColumn<?>> columns() {
    return columns;
  }

  @Override
  public CsvConfig splitor(String splitor) {
//    if (" ".equals(splitor)) {
//      throw new IllegalArgumentException("Splitor can't be \" \"");
//    }
    this.splitor = splitor;
    return this;
  }

  @Override
  public CsvConfig quoter(String quoter) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CsvConfig addColumn(CsvColumn<?> column) {
    if (findColumn(columns, column.name()).isPresent()) {
      throw new IllegalArgumentException("Column " + column.name() + " already exists.");
    }
    columns.add(column);
    return this;
  }

  @Override
  public CsvReader<List<Object>> readList() {
    return new FluentReader(this);
  }

  @Override
  public <T> CsvBeanReader<T> readBean(Class<T> bean) {
    try {
      return new FluentReader(this).asBean(bean);
    } catch (CsvException e) {
      return f -> Flowable.error(e);
    }
  }

  @Override
  public CsvWriter<List<Object>> writeList() {
    return new FluentWriter(this);
  }

  @Override
  public <T> CsvBeanWriter<T> writeBean(Class<T> bean) {
    try {
      return new FluentWriter(this).asBean(bean);
    } catch (CsvException e) {
      return f -> Flowable.error(e);
    }
  }
}
