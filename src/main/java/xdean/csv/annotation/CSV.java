package xdean.csv.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import xdean.csv.CsvValueFormatter;
import xdean.csv.CsvValueParser;

/**
 * Indicate the element is a CSV column.
 *
 * @author Dean Xu (XDean@github.com)
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, ANNOTATION_TYPE })
public @interface CSV {

  /**
   * The column name.
   * <h3>Default value</h3>
   * <ul>
   * <li>If on field, the default name is the field name.</li>
   * <li>If on method
   * <ul>
   * <li>If it's getXX/setXX/isXX, the default name is XX.</li>
   * <li>If it's other method, the default name is the method name.</li>
   * </ul>
   * </li>
   * </ul>
   */
  String name() default "";

  /**
   * The column type.
   * <h3>Default value</h3>
   * <ul >
   * <li>If on field, the default type is field type.</li>
   * <li>If on getter, the default type is the return type.</li>
   * <li>If on setter, the default type is parameter type.</li>
   * </ul>
   */
  Class<?> type() default void.class;

  /**
   * The parser to read value. The default value is from {@link CsvValueParser#forType(Class)} by
   * {@link #type()}.
   */
  @OnlyForRead
  @SuppressWarnings("rawtypes")
  Class<? extends CsvValueParser> parser() default CsvValueParser.class;

  /**
   * The parser to write value. The default value is from {@link CsvValueFormatter#toString(Class)} by
   * {@link #type()}.
   */
  @OnlyForWrite
  @SuppressWarnings("rawtypes")
  Class<? extends CsvValueFormatter> formatter() default CsvValueFormatter.class;

  /**
   * If optional, the column's absence will not lead error.
   */
  @OnlyForRead
  boolean optional() default false;

  /**
   * The default value for this column. If the column missed or the value is empty(""), default
   * value will be used.
   */
  @OnlyForRead
  String defaultValue() default "defaultValue";

}