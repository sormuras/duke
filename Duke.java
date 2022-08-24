import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
  final class Program extends ToolProgram implements Duke {
    /* TODO Introduce a new action by declaring a new method, public and no parameters.
    @Action(description = "Greet current user")
    public void hi() {
      System.out.printf("Hi %s!%n", System.getProperty("user.name", "Nobody"));
    }
    */

    /* TODO Wrap something around an existing action by overriding and super-calling it.
    @Override
    @Action(description = "BEGIN -> super.help() -> END.")
    public void help() {
      log("BEGIN");
      try {
        super.help();
      } catch (Throwable throwable) {
        logbook.log(Level.ERROR, "help() failed: " + throwable.getMessage());
        throw throwable;
      }
      log("END.");
    }
    */
  }

  /** Declares tool-related types and actions */
  class ToolProgram extends JavaProgram {
    ToolProgram() {
      super();
    }

    ToolProgram(Logbook logbook, Browser browser) {
      super(logbook, browser);
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
    final Browser browser;

    JavaProgram() {
      this(new Logbook(Default.LOGBOOK_THRESHOLD), new Browser());
    }

    JavaProgram(Logbook logbook, Browser browser) {
      this.logbook = logbook;
      this.browser = browser;
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

    @Action(
        description = "Transfer all bytes from source to target file",
        type = Action.Type.TERMINAL)
    public void load(URI source, Path target) throws Exception {
      log("load(%s) to %s".formatted(source, target.toUri()));
      var response = browser.load(source, target);
      if (response.statusCode() >= 400) throw new RuntimeException(response.toString());
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

    record Browser(HttpClient client) {
      Browser() {
        this(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
      }

      String read(URI source) throws Exception {
        var request = HttpRequest.newBuilder(source).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
      }

      HttpResponse<Path> load(URI source, Path target) throws Exception {
        if (target.toString().isBlank())
          throw new IllegalArgumentException("Target must not blank");
        var parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        var request = HttpRequest.newBuilder(source).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() >= 400) Files.deleteIfExists(target);
        return response;
      }

      HttpResponse<?> head(URI source) throws Exception {
        var request =
            HttpRequest.newBuilder(source)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.discarding());
      }
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
  static void main(String... args) throws Exception {
    var arguments = new ArrayDeque<>(args.length == 0 ? Default.ARGUMENTS : List.of(args));
    var program =
        Default.PROGRAM.isBlank()
            ? new Program()
            : (ToolProgram) Class.forName(Default.PROGRAM).getDeclaredConstructor().newInstance();
    loop:
    while (!arguments.isEmpty()) {
      var argument = arguments.removeFirst();
      switch (argument) {
        case "run", "!" -> {
          program.run(arguments.stream().toList());
          arguments.clear();
          break loop;
        }
        case "help", "?" -> program.help();
        case "load" -> {
          var source = URI.create(arguments.removeFirst());
          var target =
              arguments.isEmpty()
                  ? Path.of(source.getPath()).normalize().getFileName()
                  : Path.of(arguments.removeFirst()).normalize();
          program.load(source, target);
          break loop;
        }
        default -> program.getClass().getMethod(argument).invoke(program);
      }
    }
    if (arguments.isEmpty()) return;
    program.logbook.log(Level.WARNING, "Unhandled arguments: " + arguments);
  }

  /** Declares default constants, most of can be set via system properties. */
  interface Default {
    List<String> ARGUMENTS = List.of("help");
    String PROGRAM = property("program", "").replace('.', '$');
    Level LOGBOOK_THRESHOLD = Level.valueOf(property("logbook-threshold", "INFO"));

    private static String property(String key, String def) {
      return System.getProperty(("-Duke-" + key).substring(2), def);
    }
  }
}
