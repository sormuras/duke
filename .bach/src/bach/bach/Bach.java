package bach;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import java.util.TreeMap;
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
    var printer = new Configuration.Printer(out, err);
    var configuration = new Configuration(printer).withParsingCommandLineArguments(args);
    try {
      var bach =
          ServiceLoader.load(Creator.class)
              .findFirst()
              .orElse(DefaultAPI::new)
              .createBach(configuration);
      var welcome = "Bach " + VERSION + " [" + bach.getClass().getSimpleName() + "]";
      if (configuration.help()) {
        bach.info(welcome);
        bach.info(Configuration.USAGE_MESSAGE);
        bach.info(configuration.toString(2));
        bach.info("A call is composed of a task name and its arguments");
        bach.info(bach.tasks().toString(2));
        bach.info(welcome);
        return 0;
      }
      var calls = configuration.calls();
      if (calls.isEmpty()) {
        bach.run("list", List.of("tasks"));
        return 0;
      }
      bach.debug(welcome);
      bach.debug(configuration.toString(0));
      for (var call : calls) bach.run(call.command());
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }

  /** The command-line interface definition and also its runtime representation. */
  public record Configuration(
      Printer printer,
      Optional<Boolean> __help,
      Optional<Boolean> __verbose,
      Optional<String> __printer_threshold,
      Optional<String> __project_directory,
      List<Call> calls) {

    public static final String COMMAND_SEPARATOR = "+";

    public static final List<String> HELP_FLAGS = List.of("?", "/?", "-?", "-h", "--help");

    public static final String USAGE_MESSAGE = "Usage: bach [<options>] <calls>";

    public static boolean isFirstArgumentHelpOptionName(List<String> arguments) {
      return !arguments.isEmpty() && HELP_FLAGS.contains(arguments.get(0));
    }

    public record Printer(PrintWriter out, PrintWriter err) {}

    public record Call(List<String> command) {}

    public Configuration(Printer printer) {
      this(
          printer,
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          List.of());
    }

    public boolean help() {
      return __help.orElse(false);
    }

    public boolean verbose() {
      return __verbose.orElse(false);
    }

    public System.Logger.Level printerThreshold() {
      var defaultThreshold = verbose() ? System.Logger.Level.DEBUG : System.Logger.Level.INFO;
      return __printer_threshold.map(System.Logger.Level::valueOf).orElse(defaultThreshold);
    }

    public Path projectDirectory() {
      return __project_directory.map(Path::of).orElse(Path.of(""));
    }

    public String toString(int indent) {
      var joiner = new StringJoiner("\n");
      joiner.add("<options>");
      joiner.add("  --help = " + help());
      joiner.add("  --verbose = " + verbose());
      joiner.add("  --printer-threshold = " + printerThreshold());
      joiner.add("  --project-directory = " + projectDirectory().toUri());
      joiner.add("<calls>");
      if (calls.isEmpty()) joiner.add("  <empty>");
      calls.forEach(call -> joiner.add("  - " + String.join(" ", call.command())));
      return joiner.toString().indent(indent).stripTrailing();
    }

    public Configuration withParsingCommandLineArguments(String... args) {
      var arguments = new ArrayDeque<>(List.of(args));
      // extract components
      var help = __help.orElse(null);
      var verbose = __verbose.orElse(null);
      var printerThreshold = __printer_threshold.orElse(null);
      var projectDirectory = __project_directory.orElse(null);
      var calls = new ArrayList<>(calls());
      // handle options by parsing flags and key-value paris
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        /* parse flags */ {
          if (HELP_FLAGS.contains(argument)) {
            help = Boolean.TRUE;
            continue;
          }
          if (argument.equals("--verbose")) {
            verbose = Boolean.TRUE;
            continue;
          }
        }
        /* parse key-value pairs */ {
          int sep = argument.indexOf('=');
          var key = sep == -1 ? argument : argument.substring(0, sep);
          if (key.equals("--printer-threshold")) {
            printerThreshold = sep == -1 ? arguments.removeFirst() : argument.substring(sep + 1);
            continue;
          }
          if (key.equals("--project-directory")) {
            projectDirectory = sep == -1 ? arguments.removeFirst() : argument.substring(sep + 1);
            continue;
          }
        }
        // restore argument because first unhandled option marks the beginning of the commands
        arguments.addFirst(argument);
        break;
      }
      // parse calls from remaining arguments
      if (!arguments.isEmpty()) {
        var elements = new ArrayList<String>();
        while (true) {
          var empty = arguments.isEmpty();
          if (empty || arguments.peekFirst().equals(COMMAND_SEPARATOR)) {
            calls.add(new Call(List.copyOf(elements)));
            elements.clear();
            if (empty) break;
            arguments.removeFirst(); // consume delimiter
          }
          elements.add(arguments.removeFirst()); // consume element
        }
      }
      // compose configuration
      return new Configuration(
          printer,
          Optional.ofNullable(help),
          Optional.ofNullable(verbose),
          Optional.ofNullable(printerThreshold),
          Optional.ofNullable(projectDirectory),
          List.copyOf(calls));
    }
  }

  @FunctionalInterface
  public interface Creator {
    API createBach(Configuration configuration);
  }

  public interface API {
    Configuration configuration();

    Tasks tasks();

    default void debug(Object message) {
      if (configuration().verbose()) configuration().printer().out.println(message);
    }

    default void info(Object message) {
      configuration().printer().out.println(message);
    }

    default void run(List<String> command) {

      if (command.isEmpty()) throw new IllegalArgumentException();
      var arguments = new ArrayDeque<>(command);
      var name = arguments.removeFirst();
      run(name, arguments.stream().toList());
    }

    default void run(String name, List<String> arguments) {
      debug(">> %s %s".formatted(name, String.join(" ", arguments)));
      tasks().get(name).run(this, arguments);
    }
  }

  public static class DefaultAPI implements API {

    protected final Configuration configuration;
    protected final Tasks actions;

    public DefaultAPI(Configuration configuration) {
      this.configuration = configuration;
      this.actions = createActions();
    }

    protected Tasks createActions() {
      var actions = new ArrayList<Task>();
      ServiceLoader.load(Operator.class).forEach(operator -> actions.add(Task.of(operator)));
      ServiceLoader.load(ToolProvider.class).forEach(tool -> actions.add(Task.of(tool)));
      return new Tasks(List.copyOf(actions));
    }

    @Override
    public Tasks tasks() {
      return actions;
    }

    @Override
    public Configuration configuration() {
      return configuration;
    }
  }

  public sealed interface Task {
    String name();

    default String nick() {
      if (name().endsWith("/")) throw new IllegalStateException(name());
      return name().substring(name().lastIndexOf('/') + 1);
    }

    default boolean matches(String string) {
      return name().equals(string) || name().endsWith('/' + string);
    }

    void run(API api, List<String> arguments);

    static Task of(Operator operator) {
      return new BachOperatorTask(prefixIfNeeded(operator.name(), operator), operator);
    }

    static Task of(ToolProvider provider) {
      return new ToolProviderTask(prefixIfNeeded(provider.name(), provider), provider);
    }

    private static String prefixIfNeeded(String name, Object object) {
      if (name.indexOf('/') >= 0) return name;
      var module = object.getClass().getModule();
      var prefix = module.isNamed() ? module.getName() : object.getClass().getCanonicalName();
      return prefix + '/' + name;
    }

    record ToolProviderTask(String name, ToolProvider provider) implements Task {
      @Override
      public void run(API api, List<String> arguments) {
        var printer = api.configuration().printer();
        provider.run(printer.out(),  printer.err(), arguments.toArray(String[]::new));
      }
    }

    record BachOperatorTask(String name, Operator operator) implements Task {
      @Override
      public void run(API api, List<String> arguments) {
        operator.operate(api, arguments);
      }
    }
  }

  public record Tasks(List<Task> list) {
    public Task get(String name) {
      var found = list.stream().filter(task -> task.matches(name)).findFirst();
      if (found.isEmpty()) throw new UnsupportedOperationException(name);
      return found.get();
    }

    public String toString(int indent) {
      var joiner = new StringJoiner("\n");
      var width = 3;
      var nicks = new TreeMap<String, List<Task>>();
      for (var task : list) {
        nicks.computeIfAbsent(task.nick(), __ -> new ArrayList<>()).add(task);
        var length = task.nick().length();
        if (length > width) width = length;
      }
      var format = "%" + width + "s %s";
      for (var entry : nicks.entrySet()) {
        var names = entry.getValue().stream().map(Task::name).toList();
        joiner.add(String.format(format, entry.getKey(), names));
      }
      return joiner.toString().indent(indent).stripTrailing();
    }
  }

  @FunctionalInterface
  public interface Operator {
    void operate(API bach, List<String> arguments);

    default String name() {
      return getClass().getSimpleName();
    }
  }

  public interface Operators {
    record JarOperator(String name) implements Operator {
      public JarOperator() {
        this("jar");
      }

      @Override
      public void operate(API bach, List<String> arguments) {
        bach.info("BEGIN");
        bach.run("jdk.jartool/jar", arguments);
        bach.info("END");
      }
    }

    record ListOperator(String name) implements Operator {
      public ListOperator() {
        this("list");
      }

      @Override
      public void operate(API bach, List<String> arguments) {
        if (Configuration.isFirstArgumentHelpOptionName(arguments)) {
          bach.info("Usage: bach list ?|tasks|...");
          return;
        }
        if (arguments.isEmpty() || arguments.contains("tasks")) {
          bach.info(bach.tasks().toString(0));
        }
      }
    }
  }
}
