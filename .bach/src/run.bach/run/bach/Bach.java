package run.bach;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.bach.internal.PathSupport;

public class Bach {

  public static final String VERSION = "2022.10.02";

  public static void main(String... args) {
    System.exit(run(args));
  }

  public static int run(String... args) {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return run(out, err, args);
  }

  public static int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      var configuration = new Configuration(out, err).withParsingCommandLineArguments(args);
      var verbose = configuration.verbose();
      var version = configuration.version();
      if (verbose || version) {
        out.println("Bach " + VERSION);
        if (version) return 0;
        out.println(configuration.toString(0));
      }
      var bach = of(configuration);
      if (verbose) {
        /* Paths */ {
          bach.info("Paths");
          bach.info(bach.paths().toString(2));
        }
        /* Toolbox */ {
          bach.info("Toolbox");
          for (var finder : bach.toolbox().finders()) {
            var description = finder.description();
            var size = finder.findAll().size();
            bach.info("  %s [%2d]".formatted(description, size));
          }
          var size = bach.toolbox().finders().size();
          bach.info("    %d finder%s".formatted(size, size == 1 ? "" : "s"));
        }
      }
      if (configuration.help() || configuration.calls().isEmpty()) {
        bach.info(
                """
                Usage: bach [options] <tool> [args...] [+ <tool> [args...]]
    
                Available tools are:""");
        bach.info(bach.toolbox().toString(2));
        return 0;
      }
      for (var call : configuration.calls()) bach.run(call.command());
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }

  private static Bach of(Configuration configuration) {
    return ServiceLoader.load(Creator.class)
        .findFirst()
        .orElse(Bach::new)
        .createBach(configuration);
  }

  private final Configuration configuration;
  private final Paths paths;
  private final Browser browser;
  private final Libraries libraries;
  private final Toolbox toolbox;

  public Bach(Configuration configuration) {
    this.configuration = configuration;
    this.paths = createPaths();
    this.browser = createBrowser();
    this.libraries = createLibraries();
    this.toolbox = createToolbox();
  }

  protected Browser createBrowser() {
    return new Browser();
  }

  protected Libraries createLibraries() {
    var libraries = new ArrayList<Library>();
    ServiceLoader.load(Library.class).forEach(libraries::add);
    PathSupport.list(paths().externalModules(), PathSupport::isPropertiesFile).stream()
        .map(Library::ofProperties)
        .forEach(libraries::add);
    return new Libraries(List.copyOf(libraries));
  }

  protected Paths createPaths() {
    return Paths.ofRoot(configuration.projectDirectory());
  }

  protected Toolbox createToolbox() {
    var operators = new ArrayList<Tool>();
    ServiceLoader.load(Operator.class).forEach(it -> operators.add(Tool.of(it)));
    var providers = new ArrayList<Tool>();
    ServiceLoader.load(ToolProvider.class).forEach(it -> providers.add(Tool.of(it)));

    var finders = new ArrayList<ToolFinder>();
    finders.add(ToolFinder.of("Bach Operators", operators));
    finders.add(ToolFinder.of("Tool Providers", providers));
    return new Toolbox(List.copyOf(finders));
  }

  public final Configuration configuration() {
    return configuration;
  }

  public final Paths paths() {
    return paths;
  }

  public final Browser browser() {
    return browser;
  }

  public final Libraries libraries() {
    return libraries;
  }

  public final Toolbox toolbox() {
    return toolbox;
  }

  public void debug(Object message) {
    if (configuration().verbose()) configuration().printer().out().println(message);
  }

  public void info(Object message) {
    configuration().printer().out().println(message);
  }

  public void run(String name, List<String> arguments) {
    debug(">> %s %s".formatted(name, String.join(" ", arguments)));
    toolbox().get(name).run(this, arguments);
  }

  public void run(List<String> command) {
    if (command.isEmpty()) throw new IllegalArgumentException();
    var arguments = new ArrayDeque<>(command);
    var name = arguments.removeFirst();
    run(name, arguments.stream().toList());
  }
}
