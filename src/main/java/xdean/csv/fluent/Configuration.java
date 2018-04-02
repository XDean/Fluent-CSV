package xdean.csv.fluent;

import static xdean.jex.util.lang.ExceptionUtil.throwAsUncheck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.annotation.concurrent.Immutable;

import org.reactivestreams.Subscription;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import xdean.csv.CsvConfiguration;
import xdean.csv.CsvException;
import xdean.jex.log.Logable;

@Immutable
public class Configuration implements Logable {

  private static final BiMap<Character, Character> ESCAPE_CHARS = ImmutableBiMap.<Character, Character> builder()
      .put('b', '\b')
      .put('t', '\t')
      .put('n', '\n')
      .put('f', '\f')
      .put('r', '\r')
      .put('\'', '\'')
      .put('\\', '\\')
      .build();

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

  public String escape(String string) throws CsvException {
    try {
      return es.escape(string);
    } catch (RuntimeException e) {
      throw new CsvException(e);
    }
  }

  public List<String> split(String line) throws CsvException {
    char[] array = line.toCharArray();
    EscapeSplitSubscriber sub = new EscapeSplitSubscriber();
    Flowable.range(0, array.length)
        .map(i -> array[i])
        .subscribe(sub);
    if (sub.error != null) {
      throw sub.error;
    }
    return sub.result;
  }

  private Escaper initEscaper() {
    Escapers.Builder builder = Escapers.builder();
    ESCAPE_CHARS.forEach((literal, escape) -> {
      builder.addEscape(escape, escaper + "" + literal);
    });
    if (quoter != '\u0000') {
      builder.addEscape(quoter, escaper + "" + quoter);
    }
    builder.addEscape(splitor, escaper + "" + splitor);
    return builder.build();
  }

  public static Builder builder() {
    return new Builder();
  }

  private enum EscapeType {
    NORMAL,
    ESCAPE,
    QUOTE,
    QUOTE_ESCAPE
  }

  private final class EscapeSplitSubscriber extends AtomicLong implements FlowableSubscriber<Character> {
    private CsvException error;
    private int index;
    private EscapeType status = EscapeType.NORMAL;
    private Subscription s;
    private StringBuilder sb = new StringBuilder();
    private List<String> result = new ArrayList<>();

    @Override
    public void onSubscribe(Subscription s) {
      this.s = s;
      s.request(1);
    }

    @Override
    public void onNext(Character t) {
      char c = t.charValue();
      switch (status) {
      case NORMAL:
        if (c == quoter) {
          status = EscapeType.QUOTE;
        } else if (c == escaper) {
          status = EscapeType.ESCAPE;
        } else if (c == splitor) {
          result.add(sb.toString());
          sb.setLength(0);
        } else {
          sb.append(c);
        }
        break;
      case QUOTE_ESCAPE:
      case ESCAPE:
        if (c == quoter || c == escaper || c == splitor) {
          sb.append(c);
        } else if (ESCAPE_CHARS.containsKey(c)) {
          sb.append(ESCAPE_CHARS.get(c));
        } else {
          onError(new CsvException("'%s' cannot be escaped. (on position %d)", c, index));
        }
        status = (status == EscapeType.QUOTE_ESCAPE ? EscapeType.QUOTE : EscapeType.NORMAL);
        break;
      case QUOTE:
        if (c == quoter) {
          status = EscapeType.NORMAL;
        } else if (c == escaper) {
          status = EscapeType.QUOTE_ESCAPE;
        } else {
          sb.append(c);
        }
        break;
      }
      index++;
      s.request(1);
    }

    @Override
    public void onError(Throwable e) {
      s.cancel();
      if (e instanceof CsvException) {
        this.error = (CsvException) e;
      } else {
        throwAsUncheck(e);
      }
    }

    @Override
    public void onComplete() {
      switch (status) {
      case NORMAL:
        result.add(sb.toString());
        break;
      case ESCAPE:
      case QUOTE_ESCAPE:
        onError(new CsvException("Can't end with escaper."));
        break;
      case QUOTE:
        onError(new CsvException("The quote must close."));
        break;
      }
    }
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
