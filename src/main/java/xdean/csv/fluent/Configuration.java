package xdean.csv.fluent;

import java.util.regex.Pattern;

import javax.annotation.concurrent.Immutable;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

import xdean.csv.CsvConfiguration;

@Immutable
public class Configuration extends Escaper {
  public final char escaper;
  public final char quoter;
  public final char splitor;

  public final String regexSplitor;

  private final Escaper es;

  private Configuration(char escaper, char quoter, char splitor) {
    this.escaper = escaper;
    this.quoter = quoter;
    this.splitor = splitor;
    this.regexSplitor = Pattern.quote(splitor + "");
    this.es = initEscaper();
  }

  @Override
  public String escape(String string) {
    return es.escape(string);
  }

  private Escaper initEscaper() {
    Escapers.Builder builder = Escapers.builder()
        .addEscape('\b', escaper + "b")
        .addEscape('\t', escaper + "t")
        .addEscape('\n', escaper + "n")
        .addEscape('\f', escaper + "f")
        .addEscape('\r', escaper + "r")
        .addEscape('\"', escaper + "\"")
        .addEscape('\'', escaper + "\'")
        .addEscape('\\', escaper + "\\");
    if (quoter != '\u0000') {
      builder.addEscape(quoter, escaper + "" + quoter);
    }
    builder.addEscape(splitor, escaper + "" + splitor);
    return builder.build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private char escaper = CsvConfiguration.DEFAULT_ESCAPER;
    private char quoter = CsvConfiguration.DEFAULT_QUOTER;
    private char splitor = CsvConfiguration.DEFAULT_SPLITOR;

    public Builder escaper(char escaper) {
      this.escaper = escaper;
      return this;
    }

    public Builder quoter(char quoter) {
      this.quoter = quoter;
      return this;
    }

    public Builder splitor(char splitor) {
      this.splitor = splitor;
      return this;
    }

    public Configuration build() {
      return new Configuration(escaper, quoter, splitor);
    }
  }
}
