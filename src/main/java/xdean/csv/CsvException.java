package xdean.csv;

import xdean.codecov.CodecovIgnore;

/**
 * CSV Exception.
 *
 * @author Dean Xu (XDean@github.com)
 */
@CodecovIgnore
public class CsvException extends Exception {
  /**
   * {@link Exception#Exception()}
   */
  public CsvException() {
    super();
  }

  /**
   * {@link Exception#Exception(String, Throwable)}
   */
  public CsvException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * {@link Exception#Exception(String)}
   */
  public CsvException(String message) {
    super(message);
  }

  /**
   * {@link Exception#Exception(Throwable)}
   */
  public CsvException(Throwable cause) {
    super(cause);
  }

  /**
   * @see #CsvException(String)
   * @see String#format(String, Object...)
   */
  public CsvException(String format, Object... args) {
    super(String.format(format, args));
  }
}
