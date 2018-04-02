package xdean.csv.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target({ ANNOTATION_TYPE, TYPE })
public @interface CsvConfig {
  String escaper() default "\\";

  String quoter() default "";

  String splitor() default ",";
}
