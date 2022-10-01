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

  public static final String VERSION = "2022.10.01";

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
    try {
      var configuration = new Configuration(out, err).withParsingCommandLineArguments(args);
      var bach = configuration.createBach();
      var welcome = "Bach " + VERSION + " [" + bach.getClass().getSimpleName() + "]";
      if (configuration.help()) {
        bach.info(welcome);
        bach.info(Configuration.USAGE_MESSAGE);
        bach.info(configuration.toString(2));
        bach.info("A tool call is composed of a tool name and its arguments");
        bach.info(bach.toolbox().toString(2));
        bach.info(welcome);
        return 0;
      }
      var calls = configuration.calls();
      if (calls.isEmpty()) {
        bach.run("list", List.of("tools"));
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

    public record Printer(PrintWriter out, PrintWriter err) {}

    public record Call(List<String> command) {}

    @FunctionalInterface
    public interface Creator {
      API createBach(Configuration configuration);
    }

    public static final String COMMAND_SEPARATOR = "+";

    public static final List<String> HELP_FLAGS = List.of("?", "/?", "-?", "-h", "--help");

    public static final String USAGE_MESSAGE = "Usage: bach [<options>] <calls>";

    public static boolean isFirstArgumentHelpOptionName(List<String> arguments) {
      return !arguments.isEmpty() && HELP_FLAGS.contains(arguments.get(0));
    }

    public Configuration(PrintWriter out, PrintWriter err) {
      this(
          new Printer(out, err),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          List.of());
    }

    public API createBach() {
      return ServiceLoader.load(Creator.class).findFirst().orElse(DefaultAPI::new).createBach(this);
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

  public interface API {
    @FunctionalInterface
    interface Operator {
      void operate(API bach, List<String> arguments);

      default String name() {
        return getClass().getSimpleName();
      }
    }

    Configuration configuration();

    Toolbox toolbox();

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
      toolbox().get(name).run(this, arguments);
    }
  }

  public static class DefaultAPI implements API {

    protected final Configuration configuration;
    protected final Toolbox toolbox;

    public DefaultAPI(Configuration configuration) {
      this.configuration = configuration;
      this.toolbox = createToolbox();
    }

    protected Toolbox createToolbox() {
      var tools = new ArrayList<Tool>();
      ServiceLoader.load(API.Operator.class).forEach(operator -> tools.add(Tool.of(operator)));
      ServiceLoader.load(ToolProvider.class).forEach(provider -> tools.add(Tool.of(provider)));
      return new Toolbox(List.copyOf(tools));
    }

    @Override
    public Toolbox toolbox() {
      return toolbox;
    }

    @Override
    public Configuration configuration() {
      return configuration;
    }

    public record JarOperator(String name) implements Operator {
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

    public record ListOperator(String name) implements Operator {
      public ListOperator() {
        this("list");
      }

      @Override
      public void operate(API bach, List<String> arguments) {
        if (Configuration.isFirstArgumentHelpOptionName(arguments)) {
          bach.info("Usage: bach list ?|tools|...");
          return;
        }
        if (arguments.isEmpty() || arguments.contains("tools")) {
          bach.info(bach.toolbox().toString(0));
        }
      }
    }
  }

  public sealed interface Tool {
    String name();

    default String nick() {
      if (name().endsWith("/")) throw new IllegalStateException(name());
      return name().substring(name().lastIndexOf('/') + 1);
    }

    default boolean matches(String string) {
      return name().equals(string) || name().endsWith('/' + string);
    }

    void run(API api, List<String> arguments);

    static Tool of(API.Operator operator) {
      return new BachOperatorTool(prefixIfNeeded(operator.name(), operator), operator);
    }

    static Tool of(ToolProvider provider) {
      return new ToolProviderTool(prefixIfNeeded(provider.name(), provider), provider);
    }

    private static String prefixIfNeeded(String name, Object object) {
      if (name.indexOf('/') >= 0) return name;
      var module = object.getClass().getModule();
      var prefix = module.isNamed() ? module.getName() : object.getClass().getCanonicalName();
      return prefix + '/' + name;
    }

    record ToolProviderTool(String name, ToolProvider provider) implements Tool {
      @Override
      public void run(API api, List<String> arguments) {
        var printer = api.configuration().printer();
        provider.run(printer.out(), printer.err(), arguments.toArray(String[]::new));
      }
    }

    record BachOperatorTool(String name, API.Operator operator) implements Tool {
      @Override
      public void run(API api, List<String> arguments) {
        operator.operate(api, arguments);
      }
    }
  }

  public record Toolbox(List<Tool> list) {
    public Tool get(String string) {
      var found = list.stream().filter(tool -> tool.matches(string)).findFirst();
      if (found.isEmpty()) throw new UnsupportedOperationException(string);
      return found.get();
    }

    public String toString(int indent) {
      var joiner = new StringJoiner("\n");
      var width = 3;
      var nicks = new TreeMap<String, List<Tool>>();
      for (var tool : list) {
        nicks.computeIfAbsent(tool.nick(), __ -> new ArrayList<>()).add(tool);
        var length = tool.nick().length();
        if (length > width) width = length;
      }
      var format = "%" + width + "s %s";
      for (var entry : nicks.entrySet()) {
        var names = entry.getValue().stream().map(Tool::name).toList();
        joiner.add(String.format(format, entry.getKey(), names));
      }
      return joiner.toString().indent(indent).stripTrailing();
    }
  }
}
