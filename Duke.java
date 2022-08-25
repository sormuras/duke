import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.module.Configuration.resolveAndBind;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.spi.ToolProvider;
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

    final ToolFinder finder;

    ToolProgram() {
      super();
      this.finder =
          ToolFinder.compose(
              ToolFinder.ofToolsInModulePath(Path.of(".bach", "external-modules")),
              ToolFinder.ofJavaTools(Path.of(".bach", "external-tools")),
              ToolFinder.ofSystemTools(),
              ToolFinder.ofNativeToolsInJavaHome("java"));
    }

    ToolProgram(Logbook logbook, Browser browser, ToolFinder finder) {
      super(logbook, browser);
      this.finder = finder;
    }

    @Override
    @Action(description = "Print this help message text")
    public void help() {
      super.help();
      out("");
      out("Available Tools");
      showTools();
    }

    @Action(
        description = "Find a tool by its name and run it with an arbitrary amount of arguments",
        type = Action.Type.TERMINAL)
    public void run(List<String> command) {
      run(ToolCall.of(command));
    }

    public void run(ToolCall call) {
      log("%s".formatted(call.toCommandLine()));

      var name = call.name();
      var args = call.arguments().toArray(String[]::new);
      var tools = finder.find(name);
      if (tools.isEmpty()) throw new RuntimeException("Tool not found: " + name);
      var provider = tools.get(0).provider();
      var result = provider.run(logbook.out, logbook.err, args);
      if (result != 0) {
        throw new RuntimeException("%s returned non-zero exit code: %d".formatted(name, result));
      }
    }

    @Action(description = "Print a listing of findable tools")
    public void showTools() {
      out(finder.listing());
    }

    /** A tool reference. */
    public record Tool(String name, ToolProvider provider) {

      public static Tool of(ToolProvider provider) {
        var module = provider.getClass().getModule();
        var name = module.isNamed() ? module.getName() + '/' + provider.name() : provider.name();
        return new Tool(name, provider);
      }

      public static Tool ofNativeToolInJavaHome(String name) {
        var executable = Path.of(System.getProperty("java.home"), "bin", name);
        return ofNativeTool("java-home/" + name, List.of(executable.toString()));
      }

      public static Tool ofNativeTool(String name, List<String> command) {
        return Tool.of(new NativeToolProvider(name, command));
      }

      public boolean isNameMatching(String text) {
        // name = "foo/bar" matches text = "foo/bar"
        // name = "foo/bar" matches text = "bar" because name ends with "/bar"
        // name = "foo/bar@123" also matches = "bar"
        return name.equals(text) || name.endsWith('/' + text) || name.contains('/' + text + '@');
      }

      record NativeToolProvider(String name, List<String> command) implements ToolProvider {
        record LinePrinter(InputStream stream, PrintWriter writer) implements Runnable {
          @Override
          public void run() {
            new BufferedReader(new InputStreamReader(stream)).lines().forEach(writer::println);
          }
        }

        @Override
        public int run(PrintWriter out, PrintWriter err, String... arguments) {
          var builder = new ProcessBuilder(new ArrayList<>(command));
          builder.command().addAll(List.of(arguments));
          try {
            var process = builder.start();
            new Thread(new LinePrinter(process.getInputStream(), out)).start();
            new Thread(new LinePrinter(process.getErrorStream(), err)).start();
            return process.waitFor();
          } catch (Exception exception) {
            exception.printStackTrace(err);
            return -1;
          }
        }
      }
    }

    /**
     * A finder of tools.
     *
     * <p>What {@link java.lang.module.ModuleFinder ModuleFinder} is to {@link
     * java.lang.module.ModuleReference ModuleReference}, is {@link ToolFinder} to {@link Tool}.
     */
    @FunctionalInterface
    public interface ToolFinder {

      List<Tool> findAll();

      default List<Tool> find(String name) {
        return find(name, Tool::isNameMatching);
      }

      default List<Tool> find(String name, BiPredicate<Tool, String> filter) {
        return findAll().stream().filter(tool -> filter.test(tool, name)).toList();
      }

      static ToolFinder of(Tool... tools) {
        record DirectToolFinder(List<Tool> findAll) implements ToolFinder {}
        return new DirectToolFinder(List.of(tools));
      }

      static ToolFinder of(ClassLoader loader) {
        return ToolFinder.of(ServiceLoader.load(ToolProvider.class, loader));
      }

      static ToolFinder of(ServiceLoader<ToolProvider> loader) {
        record ServiceLoaderToolFinder(ServiceLoader<ToolProvider> loader) implements ToolFinder {
          @Override
          public List<Tool> findAll() {
            synchronized (loader) {
              return loader.stream().map(ServiceLoader.Provider::get).map(Tool::of).toList();
            }
          }
        }
        return new ServiceLoaderToolFinder(loader);
      }

      static ToolFinder of(ModuleFinder finder, boolean assertions, String... roots) {
        return of(layer(finder, assertions, roots));
      }

      static ModuleLayer layer(ModuleFinder finder, boolean assertions, String... roots) {
        var parentClassLoader = ToolFinder.class.getClassLoader();
        var parentModuleLayer = ModuleLayer.boot();
        var parents = List.of(parentModuleLayer.configuration());
        var configuration = resolveAndBind(ModuleFinder.of(), parents, finder, Set.of(roots));
        var layers = List.of(parentModuleLayer);
        var controller = defineModulesWithOneLoader(configuration, layers, parentClassLoader);
        var layer = controller.layer();
        if (assertions)
          for (var root : roots) layer.findLoader(root).setDefaultAssertionStatus(true);
        return layer;
      }

      static ToolFinder of(ModuleLayer layer) {
        record ServiceLoaderToolFinder(ModuleLayer layer, ServiceLoader<ToolProvider> loader)
            implements ToolFinder {
          @Override
          public List<Tool> findAll() {
            synchronized (loader) {
              return loader.stream()
                  .filter(service -> service.type().getModule().getLayer() == layer)
                  .map(ServiceLoader.Provider::get)
                  .map(Tool::of)
                  .toList();
            }
          }
        }
        var loader = ServiceLoader.load(layer, ToolProvider.class);
        return new ServiceLoaderToolFinder(layer, loader);
      }

      static ToolFinder ofToolsInModulePath(Path... paths) {
        return ofToolFinderSupplier(() -> of(ModuleFinder.of(paths), false));
      }

      static ToolFinder ofSystemTools() {
        return ToolFinder.of(ClassLoader.getSystemClassLoader());
      }

      static ToolFinder ofNativeToolsInJavaHome(String name, String... more) {
        var tools = new ArrayList<Tool>();
        tools.add(Tool.ofNativeToolInJavaHome(name));
        Stream.of(more).map(Tool::ofNativeToolInJavaHome).forEach(tools::add);
        return ToolFinder.of(tools.toArray(Tool[]::new));
      }

      static ToolFinder ofJavaTools(Path directory) {
        var java = Path.of(System.getProperty("java.home"), "bin", "java");
        return ofJavaTools(directory, java, "java.args");
      }

      static ToolFinder ofJavaTools(Path directory, Path java, String argsfile) {
        record ProgramToolFinder(Path path, Path java, String argsfile) implements ToolFinder {

          @Override
          public List<Tool> findAll() {
            return find(path.normalize().toAbsolutePath().getFileName().toString());
          }

          @Override
          public List<Tool> find(String name) {
            var directory = path.normalize().toAbsolutePath();
            if (!Files.isDirectory(directory)) return List.of();
            var namespace = path.getParent().getFileName().toString();
            if (!name.equals(directory.getFileName().toString())) return List.of();
            var command = new ArrayList<String>();
            command.add(java.toString());
            var args = directory.resolve(argsfile);
            if (Files.isRegularFile(args)) {
              command.add("@" + args);
              return List.of(Tool.ofNativeTool(namespace + '/' + name, command));
            }
            var jars = PathSupport.list(directory, PathSupport::isJarFile);
            if (jars.size() == 1) {
              command.add("-jar");
              command.add(jars.get(0).toString());
              return List.of(Tool.ofNativeTool(namespace + '/' + name, command));
            }
            var javas = PathSupport.list(directory, PathSupport::isJavaFile);
            if (javas.size() == 1) {
              command.add(javas.get(0).toString());
              return List.of(Tool.ofNativeTool(namespace + '/' + name, command));
            }
            return List.of();
          }
        }
        record ProgramsToolFinder(Path path, Path java, String argsfile) implements ToolFinder {
          @Override
          public List<Tool> findAll() {
            return PathSupport.list(path, Files::isDirectory).stream()
                .map(directory -> new ProgramToolFinder(directory, java, argsfile))
                .map(ToolFinder::findAll)
                .flatMap(List::stream)
                .toList();
          }
        }
        return new ProgramsToolFinder(directory, java, argsfile);
      }

      static ToolFinder ofToolFinderSupplier(Supplier<ToolFinder> supplier) {
        record SupplierToolFinder(Supplier<ToolFinder> supplier) implements ToolFinder {
          @Override
          public List<Tool> findAll() {
            try {
              return supplier.get().findAll();
            } catch (FindException ignore) {
              return List.of();
            }
          }
        }
        return new SupplierToolFinder(supplier);
      }

      static ToolFinder compose(ToolFinder... finders) {
        return compose(List.of(finders));
      }

      static ToolFinder compose(List<ToolFinder> finders) {
        record CompositeToolFinder(List<ToolFinder> finders) implements ToolFinder {
          @Override
          public List<Tool> findAll() {
            return finders.stream().flatMap(finder -> finder.findAll().stream()).toList();
          }

          @Override
          public List<Tool> find(String name) {
            return finders.stream().flatMap(finder -> finder.find(name).stream()).toList();
          }
        }
        return new CompositeToolFinder(finders);
      }

      default String listing() {
        var names = new TreeMap<String, List<Tool>>();
        for (var tool : findAll()) {
          var name = tool.name().substring(tool.name().lastIndexOf('/') + 1);
          names.computeIfAbsent(name, key -> new ArrayList<>()).add(tool);
        }
        var lines = new ArrayList<String>();
        for (var entry : names.entrySet()) {
          var name = entry.getKey();
          var tools = entry.getValue();
          var first = tools.get(0);
          lines.add(describe("%20s -> %s [%s]", name, first));
          tools.stream().skip(1).forEach(tool -> lines.add(describe("%20s    %s [%s]", "", tool)));
        }
        return String.join("\n", lines);
      }

      private static String describe(String format, String name, Tool tool) {
        return format.formatted(name, tool.name(), tool.provider().getClass().getSimpleName());
      }
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

    @Action(description = "Print the SHA-256 checksum for a file or for all files of a directory")
    public void sha256(Path path) {
      if (Files.notExists(path))
        throw new IllegalArgumentException("File or directory not found: " + path);
      if (Files.isDirectory(path)) {
        PathSupport.list(path, Files::isRegularFile).stream()
            .sorted()
            .map(
                file ->
                    "%s %11s %s"
                        .formatted(
                            PathSupport.checksum(file, "SHA-256"),
                            PathSupport.checksum(file, "SIZE"),
                            PathSupport.nameOrElse(file, "?")))
            .forEach(this::out);
        return;
      }
      out(PathSupport.checksum(path, "SHA-256"));
    }

    @Action(description = "Print this help message text")
    public void help() {
      out("Usage: java Duke.java [chainable actions...] [terminal action [arguments...]]");
      out("");
      out("Available Actions");
      showActions();
    }

    @Action(
        description = "Transfer all bytes from source to target file",
        type = Action.Type.TERMINAL)
    public void load(URI source, Path target) throws Exception {
      log("load(%s) to %s".formatted(source, target.toUri()));
      var response = browser.load(source, target);
      if (response.statusCode() >= 400) throw new RuntimeException(response.toString());
    }

    public void log(String message) {
      logbook.log(Level.INFO, message);
    }

    @Action(description = "Print a listing of available actions")
    public void showActions() {
      Stream.of(getClass().getMethods())
          .filter(method -> method.isAnnotationPresent(Action.class))
          .map(
              method ->
                  "%20s [%s]"
                      .formatted(method.getName(), method.getDeclaringClass().getSimpleName()))
          .sorted()
          .forEach(this::out);
    }

    @Action(description = "Print version information")
    public void showVersion() {
      out(Default.VERSION.toString());
    }

    public void out(String message) {
      logbook.out.println(message);
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
        case "sha256" -> {
          program.sha256(Path.of(arguments.removeFirst()));
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
        case "showVersion", "-v", "--version" -> program.showVersion();
        default -> program.getClass().getMethod(argument).invoke(program);
      }
    }
    if (arguments.isEmpty()) return;
    program.logbook.log(Level.WARNING, "Unhandled arguments: " + arguments);
  }

  /** Directory- and file-related helpers. */
  interface PathSupport {

    static String checksum(Path path, String algorithm) {
      if (Files.notExists(path)) throw new RuntimeException("File not found: " + path);
      try {
        if ("size".equalsIgnoreCase(algorithm)) return Long.toString(Files.size(path));
        var md = MessageDigest.getInstance(algorithm);
        try (var source = new BufferedInputStream(new FileInputStream(path.toFile()));
            var target = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
          source.transferTo(target);
        }
        return String.format(
            "%0" + (md.getDigestLength() * 2) + "x", new BigInteger(1, md.digest()));
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }

    static boolean isJarFile(Path path) {
      return nameOrElse(path, "").endsWith(".jar") && Files.isRegularFile(path);
    }

    static boolean isJavaFile(Path path) {
      return nameOrElse(path, "").endsWith(".java") && Files.isRegularFile(path);
    }

    static List<Path> list(Path directory, DirectoryStream.Filter<? super Path> filter) {
      if (Files.notExists(directory)) return List.of();
      var paths = new TreeSet<>(Comparator.comparing(Path::toString));
      try (var stream = Files.newDirectoryStream(directory, filter)) {
        stream.forEach(paths::add);
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
      return List.copyOf(paths);
    }

    static String nameOrElse(Path path, String defaultName) {
      var normalized = path.normalize();
      var candidate = normalized.toString().isEmpty() ? normalized.toAbsolutePath() : normalized;
      var name = candidate.getFileName();
      return name != null ? name.toString() : defaultName;
    }
  }

  /** Declares default constants, most of can be set via system properties. */
  interface Default {
    Version VERSION = Version.parse("0-ea");
    List<String> ARGUMENTS = List.of("help");
    String PROGRAM = property("program", "").replace('.', '$');
    Level LOGBOOK_THRESHOLD = Level.valueOf(property("logbook-threshold", "INFO"));

    private static String property(String key, String def) {
      return System.getProperty(("-Duke-" + key).substring(2), def);
    }
  }
}
