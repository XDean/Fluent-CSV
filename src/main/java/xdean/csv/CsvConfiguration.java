package xdean.csv;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import xdean.csv.CsvReader.CsvBeanReader;
import xdean.csv.CsvWriter.CsvBeanWriter;
import xdean.csv.annotation.CSV;
import xdean.fluent.Fluent;

/**
 * Common CSV configuration for both read and write.
 *
 * @author Dean Xu (XDean@github.com)
 *
 */
public interface CsvConfiguration extends Fluent<CsvConfiguration> {

  char NO_QUOTER = '\u0000';

  char DEFAULT_ESCAPER = '\\';
  char DEFAULT_QUOTER = NO_QUOTER;
  char DEFAULT_SPLITOR = ',';

  /**
   * Escape character. Default value is {@link #DEFAULT_ESCAPER}.
   */
  CsvConfiguration escaper(char escaper);

  /**
   * Quote character, Default value is {@link #DEFAULT_QUOTER}. In quotation, only quote character
   * and escape character should be escaped.
   */
  CsvConfiguration quoter(char quoter);

  /**
   * Set splitor. The default splitor is {@link #DEFAULT_SPLITOR}. <br>
   * Some strings should not be splitor, e.g. " ". They will lead the exception.
   */
  CsvConfiguration splitor(char splitor);

  /**
   * Ignore leading space or not.
   *
   * @apiNote CSV file usually add space after splitor for beauty. Use this option to ignore it. But
   *          space in quote will not be ignored.
   */
  CsvConfiguration ignoreLeadingSpace(boolean b);

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
   * Create {@link CsvReader} who reads content as {@code List<Object}. The objects in the list have
   * the same order with {@link #columns()}(add order).
   */
  CsvReader<List<Object>> readList();

  /**
   * Create {@link CsvReader} who reads content as {@code Map<CsvColumn, Object>}.
   */
  CsvReader<Map<CsvColumn<?>, Object>> readMap();

  /**
   * Create {@link CsvBeanReader} who reads content as bean. All &#64;{@link CSV} information will
   * be loaded.
   */
  <T> CsvBeanReader<T> readBean(Class<T> bean);

  /**********************************
   * Write
   ************************************/

  /**
   * Create {@link CsvWriter} who writes content as {@code List<Object}. The objects in the list
   * should have the same order with {@link #columns()}(add order).
   */
  CsvWriter<List<Object>> writeList();

  /**
   * Create {@link CsvWriter} who writes content as {@code Map<CsvColumn, Object>}.
   */
  CsvWriter<Map<CsvColumn<?>, Object>> writeMap();

  /**
   * Create {@link CsvBeanWriter} who reads content as bean. All &#64;{@link CSV} information will
   * be loaded.
   */
  <T> CsvBeanWriter<T> writeBean(Class<T> bean);
}
