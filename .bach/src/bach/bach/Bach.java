package bach;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;

public record Bach(String name) implements ToolProvider {

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

  public Bach() {
    this("bach");
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

      action.get().run(this, arguments.stream().toList());
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
      ServiceLoader.load(ToolProvider.class).forEach(tool -> actions.add(Action.of(tool)));
      ServiceLoader.load(Operator.class).forEach(operator -> actions.add(Action.of(operator)));
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

  public sealed interface Action {
    String name();

    void run(API api, List<String> arguments);

    static Action of(Operator operator) {
      return new BachOperatorAction(operator.name(), operator);
    }

    static Action of(ToolProvider provider) {
      return new ToolProviderAction(provider.name(), provider);
    }

    record ToolProviderAction(String name, ToolProvider provider) implements Action {
      @Override
      public void run(API api, List<String> arguments) {
        provider.run(api.printer().out(), api.printer().err(), arguments.toArray(String[]::new));
      }
    }

    record BachOperatorAction(String name, Operator operator) implements Action {
      @Override
      public void run(API api, List<String> arguments) {
        operator.operate(api, arguments);
      }
    }
  }

  public record Actions(List<Action> list) {}

  @FunctionalInterface
  public interface Creator {
    API create(Printer printer);
  }

  @FunctionalInterface
  public interface Operator {
    void operate(API api, List<String> arguments);

    default String name() {
      return getClass().getSimpleName();
    }
  }

  public interface Operators {
    record ListOperator(String name) implements Operator {
      public ListOperator() {
        this("list");
      }

      @Override
      public void operate(API api, List<String> arguments) {
        if (arguments.isEmpty() || arguments.contains("actions")) {
          api.actions().list().stream().map(Action::name).sorted().forEach(api.printer()::out);
        }
      }
    }
  }
}
