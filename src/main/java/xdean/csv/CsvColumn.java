package xdean.csv;

public interface CsvColumn<T> {
  String name();

  Class<T> type();

  CsvValueParser<T> parser();

  static <T> CsvColumn<T> create(String name, CsvValueParser<T> parser) {
    return new CsvColumn<T>() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public Class<T> type() {
        return parser.type();
      }

      @Override
      public CsvValueParser<T> parser() {
        return parser;
      }
    };
  }
}
