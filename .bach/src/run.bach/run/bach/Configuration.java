package run.bach;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public record Configuration(
    Printer printer,
    Optional<Boolean> __help,
    Optional<Boolean> __verbose,
    Optional<Boolean> __version,
    Optional<String> __printer_threshold,
    Optional<String> __project_directory,
    List<Call> calls) {

  public record Printer(PrintWriter out, PrintWriter err) {}

  public record Call(List<String> command) {}

  public static final String COMMAND_SEPARATOR = "+";

  public static final List<String> HELP_FLAGS = List.of("?", "/?", "-?", "-h", "--help");

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
        Optional.empty(),
        List.of());
  }

  public boolean help() {
    return __help.orElse(false);
  }

  public boolean verbose() {
    return __verbose.orElse(false);
  }

  public boolean version() {
    return __version.orElse(false);
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
    joiner.add("  --version = " + version());
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
    var version = __verbose.orElse(null);
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
        if (argument.equals("--version")) {
          version = Boolean.TRUE;
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
        Optional.ofNullable(version),
        Optional.ofNullable(printerThreshold),
        Optional.ofNullable(projectDirectory),
        List.copyOf(calls));
  }
}
