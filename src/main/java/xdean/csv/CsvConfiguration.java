package xdean.csv;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import xdean.csv.CsvReader.CsvBeanReader;
import xdean.csv.CsvWriter.CsvBeanWriter;
import xdean.csv.annotation.CSV;

/**
 * Common CSV configuration for both read and write.
 *
 * @author Dean Xu (XDean@github.com)
 *
 */
public interface CsvConfiguration {

  char NO_QUOTER = '\u0000';

  char DEFAULT_ESCAPER = '\\';
  char DEFAULT_QUOTER = NO_QUOTER;
  char DEFAULT_SPLITOR = ',';

  /**
   * Escape character. Default value is {@link #DEFAULT_ESCAPER}.
   */
  CsvConfiguration escaper(char escaper);

  /**
   * Quote character, Default value is {@link #DEFAULT_QUOTER}. In quotation,
   * only quote character and escape character should be escaped.
   */
  CsvConfiguration quoter(char quoter);

  /**
   * Set splitor. The default splitor is {@link #DEFAULT_SPLITOR}. <br>
   * Some strings should not be splitor, e.g. " ". They will lead the exception.
   *
   * @throws IllegalArgumentException if the splitor is illegal.
   */
  CsvConfiguration splitor(char splitor) throws IllegalArgumentException;

  /**
   * Add column to the context.
   */
  CsvConfiguration addColumn(CsvColumn<?> column) throws IllegalArgumentException;

  CsvConfiguration readConfig(Class<?> clz);

  /**
   * Add several columns to the context.
   */
  default CsvConfiguration addColumns(CsvColumn<?>... columns) {
    Arrays.stream(columns).forEach(this::addColumn);
    return this;
  }

  /**
   * Get all columns.
   *
   * @apiNote This method is for default methods.
   */
  List<CsvColumn<?>> columns();

  /********************************** Read ************************************/

  /**
   * Create {@link CsvReader} who reads content as {@code List<Object}. The
   * objects in the list have the same order with {@link #columns()}(add order).
   */
  CsvReader<List<Object>> readList();

  /**
   * Create {@link CsvReader} who reads content as
   * {@code Map<CsvColumn, Object>}.
   */
  default CsvReader<Map<CsvColumn<?>, Object>> readMap() {
    List<CsvColumn<?>> columns = columns();
    return readList().map(l -> {
      Map<CsvColumn<?>, Object> map = new LinkedHashMap<>();
      for (int i = 0; i < columns.size(); i++) {
        map.put(columns.get(i), l.get(i));
      }
      return map;
    });
  }

  /**
   * Create {@link CsvBeanReader} who reads content as bean. All
   * &#64;{@link CSV} information will be loaded.
   */
  <T> CsvBeanReader<T> readBean(Class<T> bean);

  /**********************************
   * Write
   ************************************/

  /**
   * Create {@link CsvWriter} who writes content as {@code List<Object}. The
   * objects in the list should have the same order with {@link #columns()}(add
   * order).
   */
  CsvWriter<List<Object>> writeList();

  /**
   * Create {@link CsvWriter} who writes content as
   * {@code Map<CsvColumn, Object>}.
   */
  default CsvWriter<Map<CsvColumn<?>, Object>> writeMap() {
    List<CsvColumn<?>> columns = columns();
    return writeList().<Map<CsvColumn<?>, Object>> map(
        m -> m.values().stream().sorted(Comparator.comparing(columns::indexOf)).collect(Collectors.toList()));
  }

  /**
   * Create {@link CsvBeanWriter} who reads content as bean. All
   * &#64;{@link CSV} information will be loaded.
   */
  <T> CsvBeanWriter<T> writeBean(Class<T> bean);
}
