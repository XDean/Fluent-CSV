package xdean.csv;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface CSV {
  String name() default "";

  Class<?> type() default void.class;

  @SuppressWarnings("rawtypes")
  Class<? extends CsvValueParser> parser() default CsvValueParser.class;

  @SuppressWarnings("rawtypes")
  Class<? extends CsvValueFormatter> formatter() default CsvValueFormatter.class;

  String defaultValue() default "defaultValue";

  boolean optional() default false;
}
