package xdean.csv.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicate the element takes effect only in write context. It can be null in read context.
 *
 * @author Dean Xu (XDean@github.com)
 *
 */
@Documented
@Retention(SOURCE)
@Target(METHOD)
public @interface OnlyForWrite {
  /**
   * If true, this value can be null even in write context.
   */
  boolean canBeNull() default false;
}