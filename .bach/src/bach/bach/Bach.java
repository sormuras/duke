package bach;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Bach(String name) implements ToolProvider {

  public static final String VERSION = "2022.10.02";

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
      var verbose = configuration.verbose();
      var version = configuration.version();
      if (verbose || version) {
        out.println("Bach " + VERSION);
        if (version) return 0;
        out.println(configuration.toString(0));
      }
      var bach = API.of(configuration);
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

  public sealed interface Basic {
    Configuration configuration();

    Paths paths();

    default void debug(Object message) {
      if (configuration().verbose()) configuration().printer().out.println(message);
    }

    default void info(Object message) {
      configuration().printer().out.println(message);
    }

    /** All about folders and files. */
    record Paths(Path root, Path out) {
      public static Paths ofRoot(Path root) {
        return new Paths(root, root.resolve(".bach/out"));
      }

      public Path root(String first, String... more) {
        return root.resolve(Path.of(first, more));
      }

      public Path out(String first, String... more) {
        return out.resolve(Path.of(first, more));
      }

      public Path externalModules() {
        return root(".bach", "external-modules");
      }

      public Path externalModules(String first, String... more) {
        return externalModules().resolve(Path.of(first, more));
      }

      public Path externalTools() {
        return root(".bach", "external-tools");
      }

      public Path externalTools(String first, String... more) {
        return externalTools().resolve(Path.of(first, more));
      }

      public Path javaHome() {
        return Path.of(System.getProperty("java.home"));
      }

      public String toString(int indent) {
        return """
            root             = %s
            out              = %s
            external-modules = %s
            external-tools   = %s
            """
            .formatted(
                root("").toUri(),
                out("").toUri(),
                externalModules("").toUri(),
                externalTools("").toUri())
            .indent(indent)
            .stripTrailing();
      }
    }

    interface ModulesSupport {
      static void consumeAllNames(ModuleFinder finder, Consumer<String> consumer) {
        finder.findAll().stream()
            .map(ModuleReference::descriptor)
            .map(ModuleDescriptor::toNameAndVersion)
            .sorted()
            .map(string -> string.indent(2).stripTrailing())
            .forEach(consumer);
      }

      static List<String> listMissingNames(List<ModuleFinder> finders, Set<String> more) {
        // Populate a set with all module names being in a "requires MODULE;" directive
        var requires = new TreeSet<>(more); // more required modules
        for (var finder : finders) requires.addAll(required(finder)); // main, test, and others
        // Remove names of declared modules from various module finders
        requires.removeAll(declared(ModuleFinder.ofSystem()));
        for (var finder : finders) requires.removeAll(declared(finder));
        return List.copyOf(requires);
      }

      static TreeSet<String> declared(ModuleFinder finder) {
        return declared(finder.findAll().stream().map(ModuleReference::descriptor));
      }

      static TreeSet<String> declared(Stream<ModuleDescriptor> descriptors) {
        return descriptors
            .map(ModuleDescriptor::name)
            .collect(Collectors.toCollection(TreeSet::new));
      }

      static TreeSet<String> required(ModuleFinder finder) {
        return required(finder.findAll().stream().map(ModuleReference::descriptor));
      }

      static TreeSet<String> required(Stream<ModuleDescriptor> descriptors) {
        return descriptors
            .map(ModuleDescriptor::requires)
            .flatMap(Set::stream)
            .filter(ModulesSupport::required)
            .map(ModuleDescriptor.Requires::name)
            .collect(Collectors.toCollection(TreeSet::new));
      }

      static boolean required(ModuleDescriptor.Requires requires) {
        var modifiers = requires.modifiers();
        if (modifiers.isEmpty()
            || modifiers.contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE)) return true;
        if (modifiers.contains(ModuleDescriptor.Requires.Modifier.MANDATED)) return false;
        if (modifiers.contains(ModuleDescriptor.Requires.Modifier.SYNTHETIC)) return false;
        return !modifiers.contains(ModuleDescriptor.Requires.Modifier.STATIC);
      }
    }

    record OperatingSystem(Name name, Architecture architecture) {

      public enum Name {
        ANY(".*"),
        LINUX("^linux.*"),
        MAC("^(macos|osx|darwin).*"),
        WINDOWS("^windows.*");

        private final String identifier;
        private final Pattern pattern;

        Name(String regex) {
          this.identifier = name().toLowerCase(Locale.ROOT);
          this.pattern = Pattern.compile(regex);
        }

        boolean matches(String string) {
          return pattern.matcher(string).matches();
        }

        @Override
        public String toString() {
          return identifier;
        }

        public static Name ofSystem() {
          return of(System.getProperty("os.name", "").toLowerCase(Locale.ROOT));
        }

        public static Name of(String string) {
          var normalized = string.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
          return Stream.of(LINUX, MAC, WINDOWS)
              .filter(platform -> platform.matches(normalized))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalArgumentException("Unknown operating system: " + string));
        }
      }

      public enum Architecture {
        ANY(".*"),
        ARM_32("^(arm|arm32)$"),
        ARM_64("^(aarch64|arm64)$"),
        X86_32("^(x8632|x86|i[3-6]86|ia32|x32)$"),
        X86_64("^(x8664|amd64|ia32e|em64t|x64)$");

        private final String identifier;
        private final Pattern pattern;

        Architecture(String regex) {
          this.identifier = name().toLowerCase(Locale.ROOT);
          this.pattern = Pattern.compile(regex);
        }

        boolean matches(String string) {
          return pattern.matcher(string).matches();
        }

        @Override
        public String toString() {
          return identifier;
        }

        public static Architecture ofSystem() {
          return of(System.getProperty("os.arch", ""));
        }

        public static Architecture of(String string) {
          var normalized = string.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
          return Stream.of(X86_64, ARM_64, X86_32, ARM_32)
              .filter(architecture -> architecture.matches(normalized))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Unknown architecture: " + string));
        }
      }

      public static final OperatingSystem ANY = new OperatingSystem(Name.ANY, Architecture.ANY);

      public static final OperatingSystem SYSTEM =
          new OperatingSystem(Name.ofSystem(), Architecture.ofSystem());

      public static OperatingSystem of(String classifier) {
        if (classifier == null || classifier.isEmpty()) return ANY;
        var index = classifier.indexOf('-');
        return index == -1
            ? new OperatingSystem(Name.of(classifier), Architecture.ANY)
            : new OperatingSystem(
                Name.of(classifier.substring(0, index)),
                Architecture.of(classifier.substring(index + 1)));
      }
    }

    interface PathSupport {
      static String checksum(Path file, String algorithm) {
        if (Files.notExists(file)) throw new RuntimeException("File not found: " + file);
        try {
          if ("size".equalsIgnoreCase(algorithm)) return Long.toString(Files.size(file));
          var md = MessageDigest.getInstance(algorithm);
          try (var source = new BufferedInputStream(new FileInputStream(file.toFile()));
              var target = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
            source.transferTo(target);
          }
          var format = "%0" + (md.getDigestLength() * 2) + "x";
          return String.format(format, new BigInteger(1, md.digest()));
        } catch (Exception exception) {
          throw new RuntimeException(exception);
        }
      }

      static boolean isJarFile(Path path) {
        return name(path, "").endsWith(".jar") && Files.isRegularFile(path);
      }

      static boolean isJavaFile(Path path) {
        return name(path, "").endsWith(".java") && Files.isRegularFile(path);
      }

      static boolean isPropertiesFile(Path path) {
        return name(path, "").endsWith(".properties") && Files.isRegularFile(path);
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

      static Properties properties(Path file) {
        var properties = new Properties();
        try {
          properties.load(new StringReader(Files.readString(file)));
        } catch (Exception exception) {
          throw new RuntimeException(exception);
        }
        return properties;
      }

      static String name(Path path, String defaultName) {
        var normalized = path.normalize();
        var candidate = normalized.toString().isEmpty() ? normalized.toAbsolutePath() : normalized;
        var name = candidate.getFileName();
        return name != null ? name.toString() : defaultName;
      }
    }
  }

  public sealed interface Browsing extends Basic {
    Browser browser();

    /** A facade for a http client instance. */
    record Browser(HttpClient client) {
      public Browser() {
        this(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
      }

      String read(URI source) {
        try {
          var request = HttpRequest.newBuilder(source).build();
          return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception exception) {
          throw new RuntimeException(exception);
        }
      }

      HttpResponse<Path> load(URI source, Path target) {
        if (target.toString().isBlank()) throw new IllegalArgumentException("Blank target!");
        try {
          var parent = target.getParent();
          if (parent != null) Files.createDirectories(parent);
          var request = HttpRequest.newBuilder(source).build();
          var response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));
          if (response.statusCode() >= 400) Files.deleteIfExists(target);
          return response;
        } catch (Exception exception) {
          throw new RuntimeException(exception);
        }
      }

      HttpResponse<?> head(URI source) {
        try {
          var publisher = HttpRequest.BodyPublishers.noBody();
          var request = HttpRequest.newBuilder(source).method("HEAD", publisher).build();
          return client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception exception) {
          throw new RuntimeException(exception);
        }
      }
    }

    record LoadFileOperator(String name) implements API.Operator {
      public LoadFileOperator() {
        this("load-file");
      }

      @Override
      public void operate(API bach, List<String> arguments) {
        if (help(bach, arguments, "<from-uri> [<to-path>]")) return;
        var uri = URI.create(arguments.get(0));
        var path = Path.of(arguments.get(1));
        var response = bach.browser().load(uri, path);
        if (Files.notExists(response.body())) throw new RuntimeException(response.toString());
      }
    }

    record LoadHeadOperator(String name) implements API.Operator {
      public LoadHeadOperator() {
        this("load-head");
      }

      @Override
      public void operate(API bach, List<String> arguments) {
        if (help(bach, arguments, "<uris...>")) return;
        for (var argument : arguments) {
          var uri = URI.create(argument);
          var head = bach.browser().head(uri);
          bach.info(head);
          for (var entry : head.headers().map().entrySet()) {
            bach.debug(entry.getKey());
            for (var line : entry.getValue()) bach.debug("  " + line);
          }
        }
      }
    }

    record LoadTextOperator(String name) implements API.Operator {
      public LoadTextOperator() {
        this("load-text");
      }

      @Override
      public void operate(API bach, List<String> arguments) {
        if (help(bach, arguments, "<uris...>")) return;
        bach.info(bach.browser().read(URI.create(arguments.get(0))));
      }
    }

    record LoadModuleOperator(String name) implements API.Operator {
      public LoadModuleOperator() {
        this("load-module");
      }

      @Override
      public void operate(API bach, List<String> arguments) {
        if (help(bach, arguments, "<module-names...>")) return;
        var externals = bach.paths().externalModules();
        with_next_module:
        for (var module : arguments) {
          if (ModuleFinder.of(externals).find(module).isPresent()) {
            bach.debug("Already");
            continue; // with next module
          }
          for (var locator : bach.libraries().list()) {
            var location = locator.locate(module);
            if (location == null) continue; // with next locator
            bach.debug("Module %s located via %s".formatted(module, locator.description()));
            var source = URI.create(location);
            var target = externals.resolve(module + ".jar");
            bach.browser().load(source, target);
            continue with_next_module;
          }
          throw new RuntimeException("Module not locatable: " + module);
        }
      }
    }

    record LoadMissingModulesOperator(String name) implements API.Operator {
      public LoadMissingModulesOperator() {
        this("load-missing-modules");
      }

      @Override
      public void operate(API bach, List<String> arguments) {
        if (help(bach, arguments, "<more-missing-module-names...>")) return;
        var externals = bach.paths().externalModules();
        var loaded = new TreeSet<String>();
        var difference = new TreeSet<String>();
        while (true) {
          var finders = List.of(ModuleFinder.of(externals)); // recreate in every loop
          var missing = ModulesSupport.listMissingNames(finders, Set.copyOf(arguments));
          if (missing.isEmpty()) break;
          bach.debug(
              "Load %d missing module%s".formatted(missing.size(), missing.size() == 1 ? "" : "s"));
          difference.retainAll(missing);
          if (!difference.isEmpty()) throw new Error("Still missing?! " + difference);
          difference.addAll(missing);
          bach.run("load-module", missing);
          loaded.addAll(missing);
        }
        bach.debug("Loaded %d module%s".formatted(loaded.size(), loaded.size() == 1 ? "" : "s"));
      }
    }
  }

  public sealed interface Tooling extends Browsing {
    Toolbox toolbox();

    void run(String name, List<String> arguments);

    default void run(List<String> command) {
      if (command.isEmpty()) throw new IllegalArgumentException();
      var arguments = new ArrayDeque<>(command);
      var name = arguments.removeFirst();
      run(name, arguments.stream().toList());
    }

    sealed interface Tool {
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

    record Toolbox(List<ToolFinder> finders) {
      public Tool get(String string) {
        for (var finder : finders) {
          var found = finder.findFirst(string);
          if (found.isEmpty()) continue;
          return found.get();
        }
        throw new UnsupportedOperationException(string);
      }

      public String toString(int indent) {
        var joiner = new StringJoiner("\n");
        var width = 3;
        var nicks = new TreeMap<String, List<Tool>>();
        for (var finder : finders) {
          for (var tool : finder.findAll()) {
            nicks.computeIfAbsent(tool.nick(), __ -> new ArrayList<>()).add(tool);
            var length = tool.nick().length();
            if (length > width) width = length;
          }
        }
        var format = "%" + width + "s %s";
        for (var entry : nicks.entrySet()) {
          var names = entry.getValue().stream().map(Tool::name).toList();
          joiner.add(String.format(format, entry.getKey(), names));
        }
        return joiner.toString().indent(indent).stripTrailing();
      }
    }

    interface ToolFinder {
      String description();

      List<Tool> findAll();

      default Optional<Tool> findFirst(String string) {
        return findAll().stream().filter(tool -> tool.matches(string)).findFirst();
      }

      static ToolFinder of(String description, List<Tool> tools) {
        record ListToolFinder(String description, List<Tool> findAll) implements ToolFinder {}
        return new ListToolFinder(description, List.copyOf(tools));
      }
    }
  }

  public sealed interface API extends Tooling {
    static API of(Configuration configuration) {
      return ServiceLoader.load(Creator.class)
          .findFirst()
          .orElse(DefaultImplementation::new)
          .createBach(configuration);
    }

    Libraries libraries();

    non-sealed class DefaultImplementation implements API {
      private final Configuration configuration;
      private final Paths paths;
      private final Browser browser;
      private final Libraries libraries;
      private final Toolbox toolbox;

      public DefaultImplementation(Configuration configuration) {
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
        ServiceLoader.load(API.Operator.class).forEach(it -> operators.add(Tool.of(it)));
        var providers = new ArrayList<Tool>();
        ServiceLoader.load(ToolProvider.class).forEach(it -> providers.add(Tool.of(it)));

        var finders = new ArrayList<ToolFinder>();
        finders.add(ToolFinder.of("Bach Operators", operators));
        finders.add(ToolFinder.of("Tool Providers", providers));
        return new Toolbox(List.copyOf(finders));
      }

      @Override
      public final Configuration configuration() {
        return configuration;
      }

      @Override
      public final Paths paths() {
        return paths;
      }

      @Override
      public final Browser browser() {
        return browser;
      }

      @Override
      public final Libraries libraries() {
        return libraries;
      }

      @Override
      public final Toolbox toolbox() {
        return toolbox;
      }

      @Override
      public void run(String name, List<String> arguments) {
        debug(">> %s %s".formatted(name, String.join(" ", arguments)));
        toolbox().get(name).run(this, arguments);
      }
    }

    @FunctionalInterface
    interface Creator {
      API createBach(Configuration configuration);
    }

    /** An external module library maps module names to their remote locations. */
    @FunctionalInterface
    interface Library {
      default String locate(String name) {
        return locate(name, OperatingSystem.SYSTEM);
      }

      String locate(String name, OperatingSystem os);

      default String description() {
        return getClass().getSimpleName();
      }

      static Library ofProperties(Path file) {
        record PropertiesLibrary(String description, Properties properties) implements Library {
          @Override
          public String locate(String module, OperatingSystem os) {
            var key = module + '|' + os.name();
            {
              var location = properties.getProperty(key + '-' + os.architecture());
              if (location != null) return location;
            }
            {
              var location = properties.getProperty(key);
              if (location != null) return location;
            }
            return properties.getProperty(module);
          }
        }
        var properties = PathSupport.properties(file);
        var annotation =
            Optional.ofNullable(properties.remove("@description"))
                .map(Object::toString)
                .orElse(PathSupport.name(file, "?").replace(".properties", ""));
        var modules = new TreeSet<String>();
        for (var name : properties.stringPropertyNames()) {
          if (name.startsWith("@")) {
            properties.remove(name);
            continue;
          }
          var index = name.indexOf('|');
          modules.add(index <= 0 ? name : name.substring(0, index));
        }
        var description =
            "%s [%d/%d] %s".formatted(annotation, modules.size(), properties.size(), file.toUri());
        return new PropertiesLibrary(description, properties);
      }
    }

    record Libraries(List<Library> list) {
      public String toString(int indent) {
        var joiner = new StringJoiner("\n");
        list.forEach(locator -> joiner.add(locator.description()));
        joiner.add("    %d librar%s".formatted(list.size(), list.size() == 1 ? "y" : "ies"));
        return joiner.toString().indent(indent).stripTrailing();
      }
    }

    @FunctionalInterface
    interface Operator {
      void operate(API bach, List<String> arguments);

      default String name() {
        return getClass().getSimpleName();
      }

      default boolean help(API bach, List<String> arguments, String options) {
        if (arguments.isEmpty() || Configuration.isFirstArgumentHelpOptionName(arguments)) {
          bach.info("Usage: bach %s [?] %s".formatted(name(), options));
          return true;
        }
        return false;
      }
    }

    record ListFilesTool(String name) implements ToolProvider {
      public ListFilesTool() {
        this("list-files");
      }

      @Override
      public int run(PrintWriter out, PrintWriter err, String... args) {
        var start = Path.of("");
        var pattern = args.length == 0 ? "*" : args[0];
        var syntaxAndPattern = pattern.contains(":") ? pattern : "glob:" + pattern;
        var matcher = start.getFileSystem().getPathMatcher(syntaxAndPattern);
        try (var stream = Files.find(start, 99, (path, attr) -> matcher.matches(path))) {
          stream.filter(path -> !start.equals(path)).forEach(out::println);
          return 0;
        } catch (Exception exception) {
          exception.printStackTrace(err);
          return 1;
        }
      }
    }

    record ListLibrariesOperator(String name) implements Operator {
      public ListLibrariesOperator() {
        this("list-libraries");
      }

      @Override
      public void operate(API bach, List<String> arguments) {
        bach.info(bach.libraries().toString(0));
      }
    }

    record ListPathsOperator(String name) implements Operator {
      public ListPathsOperator() {
        this("list-paths");
      }

      @Override
      public void operate(API bach, List<String> arguments) {
        bach.info(bach.paths().toString(0));
      }
    }

    record ListToolsOperator(String name) implements Operator {
      public ListToolsOperator() {
        this("list-tools");
      }

      @Override
      public void operate(API bach, List<String> arguments) {
        bach.info(bach.toolbox().toString(0));
      }
    }

    record ListModulesOperator(String name) implements Operator {

      public ListModulesOperator() {
        this("list-modules");
      }

      @Override
      public void operate(API bach, List<String> arguments) {
        bach.info("# Modules");
        bach.info("## Project modules");
        bach.info("    // TODO");
        // TODO bach.info("    %d project modules".formatted(project.size()));
        bach.info("## External modules in " + bach.paths().externalModules().toUri());
        var externalModuleFinder = ModuleFinder.of(bach.paths().externalModules());
        var externalModules = externalModuleFinder.findAll();
        ModulesSupport.consumeAllNames(externalModuleFinder, bach::info);
        bach.info("    %d external modules".formatted(externalModules.size()));

        bach.info("## Missing external modules");
        var missingModules =
            ModulesSupport.listMissingNames(List.of(externalModuleFinder), Set.of());
        missingModules.forEach(bach::info);
        bach.info("    %d missing external modules".formatted(missingModules.size()));

        var systemModuleFinder = ModuleFinder.ofSystem();
        bach.info("## System modules in " + bach.paths().javaHome().resolve("lib").toUri());
        var systemModules = systemModuleFinder.findAll();
        ModulesSupport.consumeAllNames(systemModuleFinder, bach::debug);
        bach.info("    %d system modules".formatted(systemModules.size()));
      }
    }

    record TreeTool(String name) implements ToolProvider {
      public TreeTool() {
        this("tree");
      }

      @Override
      public int run(PrintWriter out, PrintWriter err, String... args) {
        var start = Path.of(args.length == 0 ? "." : args[0]);
        try (var stream = Files.walk(start, 99)) {
          stream
              .filter(Files::isDirectory)
              .map(Path::normalize)
              .map(Path::toString)
              .map(name -> name.replace('\\', '/'))
              .filter(name -> !name.contains(".git/"))
              .sorted()
              .map(name -> name.replaceAll(".+?/", "  "))
              .forEach(out::println);
          return 0;
        } catch (Exception exception) {
          exception.printStackTrace(err);
          return 1;
        }
      }
    }

    record TreeCreateTool(String name) implements ToolProvider {
      public TreeCreateTool() {
        this("tree-create");
      }

      @Override
      public int run(PrintWriter out, PrintWriter err, String... args) {
        if (args.length != 1) {
          err.println("Exactly one argument expected, but got: " + args.length);
          return -1;
        }
        try {
          Files.createDirectories(Path.of(args[0]));
          return 0;
        } catch (Exception exception) {
          exception.printStackTrace(err);
          return 1;
        }
      }
    }

    record TreeDeleteTool(String name) implements ToolProvider {
      public TreeDeleteTool() {
        this("tree-delete");
      }

      @Override
      public int run(PrintWriter out, PrintWriter err, String... args) {
        if (args.length != 1) {
          err.println("Exactly one argument expected, but got: " + args.length);
          return -1;
        }
        try (var stream = Files.walk(Path.of(args[0]))) {
          var files = stream.sorted((p, q) -> -p.compareTo(q));
          for (var file : files.toArray(Path[]::new)) Files.deleteIfExists(file);
          return 0;
        } catch (Exception exception) {
          exception.printStackTrace(err);
          return 1;
        }
      }
    }
  }
}
