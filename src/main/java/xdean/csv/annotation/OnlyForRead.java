package xdean.csv.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicate the element takes effect only when read CSV.
 *
 * @author Dean Xu (XDean@github.com)
 *
 */
@Documented
@Retention(SOURCE)
@Target(METHOD)
public @interface OnlyForRead {

}