package run.bach;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.bach.internal.LoadValidator;
import run.bach.internal.PathSupport;

public class Bach {

  public static final String VERSION = "2022.10.10";

  public static void main(String... args) {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    System.exit(run(out, err, args));
  }

  private static int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      var cli = new CLI().withParsingCommandLineArguments(args);
      var printer = new Printer(cli.printerThreshold(), out, err);
      var configuration = new Configuration(cli, printer);
      var verbose = cli.verbose();
      var version = cli.version();
      if (verbose || version) {
        out.println("Bach " + VERSION);
        if (version) return 0;
        out.println(configuration.toString(0));
      }
      var bach = of(configuration);
      if (cli.help() || cli.calls().isEmpty()) {
        bach.info(
            """
            Usage: bach [options] <tool> [args...] [+ <tool> [args...]]

            Available tools are:""");
        bach.info(bach.tools().toString(2));
        return 0;
      }
      for (var call : cli.calls()) bach.run(ToolCall.of(call.command()));
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
  private final Logbook logbook;
  private final Paths paths;
  private final Browser browser;
  private final Libraries libraries;
  private final Tools tools;

  public Bach(Configuration configuration) {
    this.configuration = configuration;
    this.logbook = createLogbook();
    this.paths = createPaths();
    this.browser = createBrowser();
    this.libraries = createLibraries();
    this.tools = createTools();

    debug("Created instance of " + getClass());
    debug(toString(0));
  }

  protected Browser createBrowser() {
    return new Browser(new LoadValidator(this));
  }

  protected Libraries createLibraries() {
    var libraries = new ArrayList<Library>();
    ServiceLoader.load(Library.class).forEach(libraries::add);
    PathSupport.list(paths().externalModules(), PathSupport::isPropertiesFile).stream()
        .map(Library::ofProperties)
        .forEach(libraries::add);
    return new Libraries(List.copyOf(libraries));
  }

  protected Logbook createLogbook() {
    return new Logbook();
  }

  protected Paths createPaths() {
    return Paths.ofRoot(configuration.cli().projectDirectory());
  }

  protected Tools createTools() {
    var operators = new ArrayList<Tool>();
    ServiceLoader.load(BachOperator.class).forEach(it -> operators.add(Tool.of(it)));
    var providers = new ArrayList<Tool>();
    ServiceLoader.load(ToolProvider.class).forEach(it -> providers.add(Tool.of(it)));

    var javaHome = paths.javaHome();
    var finders = new ArrayList<ToolFinder>();
    finders.add(ToolFinder.ofTools("Bach Operator Services", operators));
    finders.add(ToolFinder.ofTools("Tool Provider Services", providers));
    finders.add(
        ToolFinder.ofToolProviders(
            "Tool Providers in " + paths.externalModules().toUri(), paths.externalModules()));
    finders.add(
        ToolFinder.ofJavaPrograms(
            "Java Programs in " + paths.externalTools().toUri(),
            paths.externalTools(),
            javaHome.resolve("bin").resolve("java")));
    finders.add(
        ToolFinder.ofNativeTools(
            "Native Tools in ${JAVA_HOME} -> " + javaHome.toUri(),
            name -> "java.home/" + name, // ensure stable names with synthetic prefix
            javaHome.resolve("bin"),
            "java",
            "jfr",
            "jdeprscan"));
    return new Tools(List.copyOf(finders));
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

  public final Logbook logbook() {
    return logbook;
  }

  public final Tools tools() {
    return tools;
  }

  public void debug(Object message) {
    log(System.Logger.Level.DEBUG, message);
  }

  public void info(Object message) {
    log(System.Logger.Level.INFO, message);
  }

  public void log(System.Logger.Level level, Object message) {
    var text = String.valueOf(message);
    logbook().log(level, text);
    configuration().printer().println(level, text);
  }

  public void run(String tool, String... args) {
    run(new ToolCall(tool, List.of(args)));
  }

  public void run(String tool, List<String> arguments) {
    run(new ToolCall(tool, List.copyOf(arguments)));
  }

  public void run(String tool, ToolCall.Composer composer) {
    run(composer.apply(new ToolCall(tool)));
  }

  public void run(ToolCall call) {
    var id = Thread.currentThread().getId();
    var name = call.name();
    var arguments = call.arguments();
    info(">> [%03x] %s %s".formatted(id, name, String.join(" ", arguments)));
    var tool = tools().get(name);
    if (tool instanceof Tool.BachOperatorTool it) {
      runBachOperator(it.operator(), arguments);
      return;
    }
    if (tool instanceof Tool.ToolProviderTool it) {
      runToolProvider(it.provider(), arguments);
      return;
    }
    throw new UnsupportedOperationException(tool.getClass().getCanonicalName());
  }

  void runBachOperator(BachOperator operator, List<String> arguments) {
    operator.operate(this, arguments);
  }

  void runToolProvider(ToolProvider provider, List<String> arguments) {
    var printer = configuration().printer();
    Thread.currentThread().setContextClassLoader(provider.getClass().getClassLoader());
    provider.run(printer.out(), printer.err(), arguments.toArray(String[]::new));
  }

  public String toString(int indent) {
    return """
            Paths
            %s
            Tool Finders
            %s
            """
        .formatted(paths.toString(indent + 2), tools.toFindersString(indent + 2))
        .indent(indent)
        .stripTrailing();
  }
}
