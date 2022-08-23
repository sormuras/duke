import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayDeque;
import java.util.List;
import java.util.stream.Stream;
import jdk.jfr.Description;

sealed interface Duke {

  final class Program extends BuildProgram implements Duke {
    // TODO Here be your dragons...
  }

  class BuildProgram extends ToolProgram {

    final Settings settings;

    BuildProgram() {
      super();
      this.settings = new Settings();
    }

    BuildProgram(Logbook logbook, Settings settings) {
      super(logbook);
      this.settings = settings;
    }

    @Description("Start standard workflow")
    public void build() {
      log("build()");
      if (settings.rebuild()) clean();
      compile();
      test();
      document();
    }

    @Description("Delete all generated files")
    public void clean() {
      log("clean()");
    }

    @Description("Create Java archives from Java source files")
    public void compile() {
      log("compile()");
    }

    @Description("Launch automated checks")
    public void test() {
      log("test()");
    }

    @Description("Generate documentation assets")
    public void document() {
      log("document()");
    }

    record Settings(boolean rebuild) {
      Settings() {
        this(Default.FORCE_REBUILD);
      }
    }
  }

  class ToolProgram extends JavaProgram {
    ToolProgram() {
      super();
    }

    ToolProgram(Logbook logbook) {
      super(logbook);
    }

    @Description("Find a tool by its name and run it with an arbitrary amount of arguments")
    public void run(List<String> command) {
      run(ToolCall.of(command));
    }

    public void run(ToolCall call) {
      log("%s".formatted(call.toCommandLine()));
    }

    /** A call consisting of a tool name and a list of arguments. */
    public record ToolCall(String name, List<String> arguments) {
      public static ToolCall of(String name, Object... arguments) {
        if (arguments.length == 0) return new ToolCall(name);
        if (arguments.length == 1) return new ToolCall(name, List.of(arguments[0].toString()));
        return new ToolCall(name).with(Stream.of(arguments));
      }

      public static ToolCall of(List<String> command) {
        var size = command.size();
        if (size == 0) throw new IllegalArgumentException("Empty command");
        var name = command.get(0);
        if (size == 1) return new ToolCall(name);
        if (size == 2) return new ToolCall(name, List.of(command.get(1).trim()));
        return new ToolCall(name).with(command.stream().skip(1).map(String::trim));
      }

      public ToolCall(String name) {
        this(name, List.of());
      }

      public ToolCall with(Stream<?> objects) {
        var strings = objects.map(Object::toString);
        return new ToolCall(name, Stream.concat(arguments.stream(), strings).toList());
      }

      public ToolCall with(Object argument) {
        return with(Stream.of(argument));
      }

      public ToolCall with(String key, Object value, Object... values) {
        var call = with(Stream.of(key, value));
        return values.length == 0 ? call : call.with(Stream.of(values));
      }

      public ToolCall withFindFiles(String glob) {
        return withFindFiles(Path.of(""), glob);
      }

      public ToolCall withFindFiles(Path start, String glob) {
        return withFindFiles(start, "glob", glob);
      }

      public ToolCall withFindFiles(Path start, String syntax, String pattern) {
        var syntaxAndPattern = syntax + ':' + pattern;
        var matcher = start.getFileSystem().getPathMatcher(syntaxAndPattern);
        return withFindFiles(start, Integer.MAX_VALUE, matcher);
      }

      public ToolCall withFindFiles(Path start, int maxDepth, PathMatcher matcher) {
        try (var files = Files.find(start, maxDepth, (p, a) -> matcher.matches(p))) {
          return with(files);
        } catch (Exception exception) {
          throw new RuntimeException("Find files failed in: " + start, exception);
        }
      }

      String toCommandLine() {
        if (arguments.isEmpty()) return name;
        if (arguments.size() == 1) return name + ' ' + arguments.get(0);
        if (arguments.size() == 2) return name + ' ' + arguments.get(0) + ' ' + arguments.get(1);
        return name + ' ' + String.join(" ", arguments);
      }
    }
  }

  class JavaProgram {
    final Logbook logbook;

    JavaProgram() {
      this(new Logbook(Default.LOGBOOK_THRESHOLD));
    }

    JavaProgram(Logbook logbook) {
      this.logbook = logbook;
    }

    @Description("Print this help message text")
    public void help() {
      log("Usage: ... ");
      Class<?> current = getClass();
      while (!Object.class.equals(current)) {
        var names =
            Stream.of(current.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Description.class))
                .map(
                    method ->
                        method.getName() + " - " + method.getAnnotation(Description.class).value())
                .sorted()
                .toList();
        if (!names.isEmpty()) {
          System.out.println(current.getSimpleName());
          names.forEach(name -> System.out.println("  " + name));
        }
        current = current.getSuperclass();
      }
    }

    public void log(String message) {
      logbook.log(Level.INFO, message);
    }

    record Logbook(Level threshold, PrintWriter out, PrintWriter err) {
      Logbook(Level threshold) {
        this(threshold, new PrintWriter(System.out, true), new PrintWriter(System.err, true));
      }

      void log(Level level, String message) {
        // TODO Always persist log entry.
        if (threshold == Level.OFF) return;
        var severity = level.getSeverity();
        if (threshold == Level.ALL || severity >= threshold.getSeverity()) {
          var printer = severity >= Level.ERROR.getSeverity() ? err : out;
          printer.println(message);
        }
      }
    }
  }

  static void main(String... args) {
    var arguments = new ArrayDeque<>(args.length == 0 ? Default.ARGUMENTS : List.of(args));
    try {
      var program =
          Default.PROGRAM == null
              ? new Program()
              : (BuildProgram)
                  Class.forName(Default.PROGRAM.replace('.', '$'))
                      .getDeclaredConstructor()
                      .newInstance();
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        switch (argument) {
          case "build" -> program.build();
          case "run", "!" -> {
            program.run(arguments.stream().toList());
            arguments.clear();
            return;
          }
          case "help", "?" -> program.help();
          default -> program.getClass().getMethod(argument).invoke(program);
        }
      }
    } catch (ReflectiveOperationException exception) {
      throw new RuntimeException(exception);
    }
  }

  interface Default {
    List<String> ARGUMENTS = List.of("help");
    String PROGRAM = property("program", null);
    Level LOGBOOK_THRESHOLD = Level.valueOf(property("logbook-threshold", "INFO"));
    boolean FORCE_REBUILD = Boolean.parseBoolean(property("force-rebuild", "false"));

    private static String property(String key, String def) {
      return System.getProperty(("-Duke-" + key).substring(2), def);
    }
  }
}
