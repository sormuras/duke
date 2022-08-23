import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayDeque;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

/** An enhanced entry-point exploration. */
sealed interface Duke {

  /** Your configuration, your types, your actions, your program. */
  final class Program extends BuildProgram implements Duke {
    /* TODO Introduce a new action by declaring a new method, public and no parameters.
    @Action("Greet current user")
    public void hi() {
      System.out.printf("Hi %s!%n", System.getProperty("user.name", "Nobody"));
    }
    */

    /* TODO Wrap something around an existing action by overriding and super-calling it.
    @Override
    public void build() {
      log("BEGIN");
      try {
        super.build();
      } catch (Throwable throwable) {
        logbook.log(Level.ERROR, "build() failed: " + throwable.getMessage());
        throw throwable;
      }
      log("END.");
    }
    */

    /* TODO Rewrite an action by overriding and not calling it.
    @Override
    public void test() {
      log("test()");
      if (Math.random() < 0.5d) throw new RuntimeException("Test failed. Because.");
    }
    */
  }

  /** Declares project-related types and actions. */
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

    @Action(description = "Start standard workflow")
    public void build() {
      log("build()");
      if (settings.rebuild()) clean();
      compile();
      test();
      document();
    }

    @Action(description = "Delete all generated files")
    public void clean() {
      log("clean()");
    }

    @Action(description = "Create Java archives from Java source files")
    public void compile() {
      log("compile()");
    }

    @Action(description = "Launch automated checks")
    public void test() {
      log("test()");
    }

    @Action(description = "Generate documentation assets")
    public void document() {
      log("document()");
    }

    record Settings(boolean rebuild) {
      Settings() {
        this(Default.FORCE_REBUILD);
      }
    }
  }

  /** Declares tool-related types and actions */
  class ToolProgram extends JavaProgram {
    ToolProgram() {
      super();
    }

    ToolProgram(Logbook logbook) {
      super(logbook);
    }

    @Action(
        description = "Find a tool by its name and run it with an arbitrary amount of arguments",
        type = Action.Type.TERMINAL)
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

  /** Declares common types and actions. */
  class JavaProgram {
    final Logbook logbook;

    JavaProgram() {
      this(new Logbook(Default.LOGBOOK_THRESHOLD));
    }

    JavaProgram(Logbook logbook) {
      this.logbook = logbook;
    }

    @Action(description = "Print this help message text")
    public void help() {
      log("Usage: java Duke.java [chainable actions...] [terminal action [arguments...]]");
      // List available actions, grouped by their declaring class and sorted by name
      Class<?> current = getClass();
      while (!Object.class.equals(current)) {
        var actions =
            Stream.of(current.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Action.class))
                .map(this::describe)
                .sorted()
                .toList();
        if (!actions.isEmpty()) {
          var s = actions.size() == 1 ? "" : "s";
          log("%d action%s declared by %s".formatted(actions.size(), s, current.getSimpleName()));
          actions.forEach(action -> log("  " + action));
        }
        current = current.getSuperclass();
      }
    }

    protected String describe(Method method) {
      var action = method.getAnnotation(Action.class);
      return new StringJoiner(" - ")
          .add(method.getName())
          .add(action.description())
          .add(action.type().toString())
          .toString();
    }

    public void log(String message) {
      logbook.log(Level.INFO, message);
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Action {
      enum Type {
        CHAINABLE,
        TERMINAL
      }

      Type type() default Type.CHAINABLE;

      String description();
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

  /**
   * Creates a program instance and performs actions on it.
   *
   * @param args the list of actions to perform
   */
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

  /**
   * Declares default constants, most of can be set via system properties.
   */
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
