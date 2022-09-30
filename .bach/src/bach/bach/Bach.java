package bach;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;

public final class Bach implements ToolProvider {

  public static final String VERSION = "2022.09.30";

  public static void main(String... args) {
    System.exit(run(args));
  }

  public static int run(String... args) {
    var bach = new Bach();
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return bach.run(out, err, args);
  }

  public Bach() {}

  @Override
  public String name() {
    return "bach";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var printer = new Printer(out, err);
    try {
      var bach = API.of(printer);
      printer.out("Bach " + VERSION + " [" + bach.getClass().getSimpleName() + "]");
      if (args.length == 0) return 0;
      bach.run(args);
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }

  public interface API {
    Printer printer();

    default void run(String... args) {
      var command = List.of(args);
      var printer = printer();

      printer.out("| " + String.join(" ", command));

      if (command.isEmpty()) throw new IllegalArgumentException();
      var arguments = new ArrayDeque<>(command);
      var name = arguments.removeFirst();

      var tool = ToolProvider.findFirst(name);
      if (tool.isPresent()) {
        tool.get().run(printer.out, printer.err, arguments.toArray(String[]::new));
        return;
      }

      var operator = Operator.findFirst(name);
      if (operator.isPresent()) {
          operator.get().operate(this, arguments.toArray(String[]::new));
          return;
      }

      throw new UnsupportedOperationException(name);
    }

    static API of(Printer printer) {
      return ServiceLoader.load(Creator.class).findFirst().orElse(DefaultAPI::new).create(printer);
    }
  }

  public static class DefaultAPI implements API {

    protected final Printer printer;

    public DefaultAPI(Printer printer) {
      this.printer = printer;
    }

    @Override
    public Printer printer() {
      return printer;
    }
  }

  public record Printer(PrintWriter out, PrintWriter err) {
    public void out(Object string) {
      out.println(string);
    }
  }

  @FunctionalInterface
  public interface Creator {
    API create(Printer printer);
  }

  @FunctionalInterface
  public interface Operator {
    void operate(API api, String... args);

    default String name() {
      return getClass().getSimpleName();
    }

    static Optional<Operator> findFirst(String name) {
      return ServiceLoader.load(Operator.class).stream()
              .map(ServiceLoader.Provider::get)
              .filter(operator -> operator.name().equals(name))
              .findFirst();
    }
  }
}
