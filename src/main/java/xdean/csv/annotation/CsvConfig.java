package xdean.csv.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import xdean.csv.CsvConfiguration;

@Documented
@Retention(RUNTIME)
@Target({ ANNOTATION_TYPE, TYPE })
public @interface CsvConfig {
  char escaper() default CsvConfiguration.DEFAULT_ESCAPER;

  char quoter() default CsvConfiguration.NO_QUOTER;

  char splitor() default CsvConfiguration.DEFAULT_SPLITOR;
}
