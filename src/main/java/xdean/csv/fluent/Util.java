package xdean.csv.fluent;

public class Util {
  public static <T extends Throwable, R> R throwIt(T t) throws T {
    throw t;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Throwable, R> R throwAsUncheck(Throwable t) throws T {
    throw (T) t;
  }
}
