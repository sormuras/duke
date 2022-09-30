package bach;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
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

  public sealed interface Action {
    String name();

    record ToolProviderAction(String name, ToolProvider provider) implements Action {}

    record BachOperatorAction(String name, Operator operator) implements Action {}
  }

  public record Actions(List<Action> list) {}

  public interface API {
    Actions actions();

    Printer printer();

    default void run(String... args) {
      var command = List.of(args);
      var printer = printer();

      printer.out("| " + String.join(" ", command));

      if (command.isEmpty()) throw new IllegalArgumentException();
      var arguments = new ArrayDeque<>(command);
      var name = arguments.removeFirst();

      var action = actions().list().stream().filter(a -> a.name().equals(name)).findFirst();
      if (action.isEmpty()) throw new UnsupportedOperationException(name);

      if (action.get() instanceof Action.ToolProviderAction tool) {
        tool.provider().run(printer.out, printer.err, arguments.toArray(String[]::new));
        return;
      }
      if (action.get() instanceof Action.BachOperatorAction operator) {
        operator.operator().operate(this, arguments.toArray(String[]::new));
        return;
      }
      throw new IllegalStateException(action.toString());
    }

    static API of(Printer printer) {
      return ServiceLoader.load(Creator.class).findFirst().orElse(DefaultAPI::new).create(printer);
    }
  }

  public static class DefaultAPI implements API {

    protected final Actions actions;
    protected final Printer printer;

    public DefaultAPI(Printer printer) {
      this.printer = printer;
      this.actions = createActions();
    }

    protected Actions createActions() {
      var actions = new ArrayList<Action>();
      ServiceLoader.load(ToolProvider.class).stream()
          .map(ServiceLoader.Provider::get)
          .forEach(
              provider -> actions.add(new Action.ToolProviderAction(provider.name(), provider)));
      Operator.findAll()
          .forEach(
              operator -> actions.add(new Action.BachOperatorAction(operator.name(), operator)));
      return new Actions(List.copyOf(actions));
    }

    @Override
    public Actions actions() {
      return actions;
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

    static List<Operator> findAll() {
      return ServiceLoader.load(Operator.class).stream().map(ServiceLoader.Provider::get).toList();
    }
  }

  public interface Operators {
    record ListOperator(String name) implements Operator {
      public ListOperator() {
        this("list");
      }

      @Override
      public void operate(API api, String... args) {
        var arguments = List.of(args);
        if (arguments.isEmpty() || arguments.contains("actions")) {
          api.actions().list().stream().map(Action::name).sorted().forEach(api.printer()::out);
        }
      }
    }
  }
}
