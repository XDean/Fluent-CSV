package xdean.csv.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

import xdean.csv.CsvValueFormatter;
import xdean.csv.CsvValueParser;

/**
 * If annotated on field or method, indicate the element is a CSV column.
 * 
 * If annotated on constructor, indicate the constructor is the primary constructor. If no
 * constructor annotated with {@code @CSV}, no-arg constructor will be used. For the constructor
 * parameters, default {@code @CSV} will be used if there is not annotated {@code @CSV}.
 * 
 * Notice if a field is also a constructor parameter, only annotated on the parameter is enough.
 *
 * @author Dean Xu (XDean@github.com)
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
public @interface CSV {

  /**
   * Alias of name
   */
  @AliasFor("name")
  String value() default "";

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
   * <li>If on constructor's parameter, the default name is parameter name.</li>
   * </ul>
   */
  @AliasFor("value")
  String name() default "";

  /**
   * The column type.
   * <h3>Default value</h3>
   * <ul >
   * <li>If on field, the default type is field type.</li>
   * <li>If on getter, the default type is the return type.</li>
   * <li>If on setter, the default type is parameter type.</li>
   * <li>If on constructor's parameter, the default type is parameter type</li>
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
   * The parser to write value. The default value is from {@link CsvValueFormatter#toString(Class)}
   * by {@link #type()}.
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