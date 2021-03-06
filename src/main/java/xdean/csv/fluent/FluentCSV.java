package xdean.csv.fluent;

import static xdean.csv.fluent.Util.findColumn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotationUtils;

import io.reactivex.Flowable;
import xdean.csv.CsvColumn;
import xdean.csv.CsvConfiguration;
import xdean.csv.CsvException;
import xdean.csv.CsvReader;
import xdean.csv.CsvReader.CsvBeanReader;
import xdean.csv.CsvWriter;
import xdean.csv.CsvWriter.CsvBeanWriter;
import xdean.csv.annotation.CsvConfig;
import xdean.jex.log.Logable;

public class FluentCSV implements CsvConfiguration, Logable {

  public static FluentCSV create() {
    return new FluentCSV();
  }

  final List<CsvColumn<?>> columns = new ArrayList<>();
  final Configuration.Builder configuration = Configuration.builder();

  private FluentCSV() {
  }

  @Override
  public List<CsvColumn<?>> columns() {
    return columns;
  }

  @Override
  public CsvConfiguration escaper(char escaper) {
    configuration.escaper(escaper);
    return this;
  }

  @Override
  public CsvConfiguration quoter(char quoter) {
    configuration.quoter(quoter);
    return this;
  }

  @Override
  public CsvConfiguration splitor(char splitor) {
    configuration.splitor(splitor);
    return this;
  }

  @Override
  public CsvConfiguration ignoreLeadingSpace(boolean b) {
    configuration.ignoreLeadingSpace(b);
    return this;
  }

  @Override
  public CsvConfiguration readConfig(Class<?> clz) {
    CsvConfig config = AnnotationUtils.getAnnotation(clz, CsvConfig.class);
    if (config == null) {
      warn("There is no @CsvConfig on " + clz);
    } else {
      escaper(config.escaper());
      quoter(config.quoter());
      splitor(config.splitor());
      ignoreLeadingSpace(config.ignoreLeadingSpace());
    }
    return this;
  }

  @Override
  public CsvConfiguration addColumn(CsvColumn<?> column) {
    if (findColumn(columns, column.name()).isPresent()) {
      throw new IllegalArgumentException("Column " + column.name() + " already exists.");
    }
    columns.add(column);
    return this;
  }

  @Override
  public CsvReader<List<Object>> readList() {
    return new FluentReader(this).mapTo(m -> m.entrySet().stream()
        .sorted(Comparator.comparing(e -> columns.indexOf(e.getKey())))
        .map(e -> e.getValue())
        .collect(Collectors.toList()));
  }

  @Override
  public CsvReader<Map<CsvColumn<?>, Object>> readMap() {
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
    return new FluentWriter(this).mapFrom(l -> {
      Map<CsvColumn<?>, Object> map = new HashMap<>();
      for (int i = 0; i < l.size(); i++) {
        map.put(columns.get(i), l.get(i));
      }
      return map;
    });
  }

  @Override
  public CsvWriter<Map<CsvColumn<?>, Object>> writeMap() {
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
