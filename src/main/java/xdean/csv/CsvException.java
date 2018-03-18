package xdean.csv;

public class CsvException extends Exception {

  public CsvException() {
    super();
  }

  public CsvException(String message, Throwable cause) {
    super(message, cause);
  }

  public CsvException(String message) {
    super(message);
  }

  public CsvException(Throwable cause) {
    super(cause);
  }

  public CsvException(String format, Object... args) {
    super(String.format(format, args));
  }
}
