import module java.base;

/** Duke's Java program and JShell interface. */
class Duke {
  static final String VERSION = "2025.11.23+23.00";

  static final String ROOTS_KEY_NAME = "@RootModuleNames";
  static final String SOURCES_KEY_NAME = "@ModulesProperties";
  static final String PINNED_KEY_PREFIX = "@ModuleLink|";

  static void main(String... args) {
    about();
    IO.println();
    if (args.length == 1) {
      writeModules();
      return;
    }
    printLinks();
    IO.println();
    resolveModules();
    IO.println();
    printModules();
  }

  static void about() {
    var text =
        """
        Duke %s
        Java %s by %s
        %s in %s
        """
            .formatted(
                VERSION,
                Runtime.version(),
                System.getProperty("java.vendor", "Unknown vendor"),
                System.getProperty("os.name", "Unknown operating system"),
                Path.of("").toUri());
    IO.print(text);
  }

  static void printLinks() {
    var file = "lib/.modules.properties";
    var links = Modules.lookup(file).links();
    var size = links.size();
    var s = size == 1 ? "" : "s";
    links.forEach(IO::println);
    IO.println("    %d link%s in %s lookup".formatted(size, s, file));
  }

  static void printModules() {
    var directory = "lib";
    var modules = Modules.folder(directory).list();
    var size = modules.size();
    var s = size == 1 ? "" : "s";
    modules.forEach(IO::println);
    IO.println("    %d module%s in %s directory".formatted(size, s, directory));
  }

  static void resolveModules() {
    var directory = "lib";
    Modules.resolver(directory).resolve();
  }

  static void writeModules() {
    var file = Path.of("lib", ".modules.properties");
    Modules.Writer.of(file).write(file);
  }

  static class Modules {
    static Folder folder(String directory) {
      return new Folder(Path.of(directory));
    }

    static Lookup lookup(String file) {
      var path = Path.of(file);
      var properties = new Properties();
      try {
        properties.load(new StringReader(Files.readString(path)));
        var links =
            properties.stringPropertyNames().stream()
                .filter(name -> !name.startsWith("@"))
                .sorted()
                .map(name -> Link.of(name, properties.getProperty(name)))
                .toList();
        var roots = List.of(properties.getProperty(ROOTS_KEY_NAME, "").split(","));
        return new Lookup(links, roots);
      } catch (Exception exception) {
        throw new RuntimeException("Loading lookup failed for: " + file, exception);
      }
    }

    static Resolver resolver(String directory) {
      var folder = Modules.folder(directory);
      var lookup = Modules.lookup(folder.directory().resolve(".modules.properties").toString());
      return new Modules.Resolver(folder, lookup);
    }

    record Folder(Path directory) {
      /// Compute observable modules and return their names.
      List<String> list() {
        var finder = ModuleFinder.of(directory);
        var modules = finder.findAll();
        return modules.stream()
            .map(ModuleReference::descriptor)
            .map(ModuleDescriptor::toNameAndVersion)
            .sorted()
            .toList();
      }

      /// Compute missing modules and return their names.
      List<String> missing() {
        var finder = ModuleFinder.of(directory);
        var universe = ModuleFinder.compose(finder, ModuleFinder.ofSystem());
        return finder.findAll().stream()
            .parallel()
            .map(ModuleReference::descriptor)
            .map(ModuleDescriptor::requires)
            .flatMap(Set::stream)
            .filter(
                requires -> {
                  var mods = requires.modifiers();
                  var isStatic = mods.contains(ModuleDescriptor.Requires.Modifier.STATIC);
                  var isTransitive = mods.contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE);
                  return !isStatic || isTransitive;
                })
            .map(ModuleDescriptor.Requires::name)
            .filter(name -> universe.find(name).isEmpty())
            .distinct()
            .sorted()
            .toList();
      }
    }

    record Link(String name, URI location) {
      static Link of(String name, String location) {
        return new Link(name, URI.create(location));
      }
    }

    record Lookup(List<Link> links, List<String> roots) {
      Optional<Link> findLinkByName(String name) {
        return links.stream().filter(link -> link.name().equals(name)).findFirst();
      }
    }

    record Resolver(Folder folder, Lookup lookup) {
      void resolve() {
        lookup.roots().stream().parallel().forEach(this::downloadModule);
        downloadAllMissingModules();
      }

      /// Download single module.
      void downloadModule(String name) {
        // Fail-fast for unknown module name
        var link = lookup.findLinkByName(name);
        if (link.isEmpty()) throw new FindException("Module name not linked: " + name);
        var source = link.get().location();
        var target = folder.directory().resolve(name + ".jar");
        // Don't overwrite existing JAR file
        if (Files.exists(target)) return;
        // Download JAR file
        try (var stream = source.toURL().openStream()) {
          IO.println(name + " <- " + source + "...");
          Files.createDirectories(target.getParent());
          Files.copy(stream, target);
          // Verify modular JAR file contains named module
          ModuleFinder.of(target).findAll().stream()
              .filter(reference -> reference.descriptor().name().equals(name))
              .findAny()
              .orElseThrow();
        } catch (IOException cause) {
          try {
            Files.deleteIfExists(target);
          } catch (Exception ignore) {
          }
          throw new UncheckedIOException(cause);
        }
      }

      /// Compute missing modules and download them transitively.
      void downloadAllMissingModules() {
        var missing = folder.missing();
        while (!missing.isEmpty()) {
          missing.stream().parallel().forEach(this::downloadModule);
          missing = folder.missing();
        }
      }
    }

    record Writer(List<URI> sources, List<Link> pins, List<String> roots) {
      static Writer of(Path file) {
        var properties = new Properties();
        try {
          properties.load(new StringReader(Files.readString(file)));
          var sources =
              Stream.of(properties.getProperty(SOURCES_KEY_NAME, "").split(","))
                  .map(String::strip)
                  .map(URI::create)
                  .toList();
          var pins =
              properties.stringPropertyNames().stream()
                  .filter(name -> name.startsWith(PINNED_KEY_PREFIX))
                  .map(name -> name.substring(PINNED_KEY_PREFIX.length()))
                  .sorted()
                  .map(name -> Link.of(name, properties.getProperty(name)))
                  .toList();
          var roots =
              Stream.of(properties.getProperty(ROOTS_KEY_NAME, "").split(","))
                  .map(String::strip)
                  .toList();
          return new Writer(sources, pins, roots);
        } catch (Exception exception) {
          throw new RuntimeException("Loading lookup failed for: " + file, exception);
        }
      }

      void write(Path target) {
        var lines = new ArrayList<String>();
        //
        var rootsJoiner = new StringJoiner(",\\\n", ROOTS_KEY_NAME + "=\\\n", "");
        for (var root : roots) rootsJoiner.add("  " + root);
        lines.add(rootsJoiner.toString());
        //
        var sourcesJoiner = new StringJoiner(",\\\n", SOURCES_KEY_NAME + "=\\\n", "");
        for (var source : sources) sourcesJoiner.add("  " + source);
        lines.add(sourcesJoiner.toString());
        // Write pinned links and seed link map with them.
        var map = new TreeMap<String, Link>();
        for (var pin : pins) {
          lines.add(PINNED_KEY_PREFIX + pin.name() + "=" + pin.location());
          var old = map.put(pin.name(), pin);
          if (old != null) throw new AssertionError("Duplicate pinned link: " + pin);
        }
        var duplicates = new ArrayList<Link>();
        for (var source : sources) {
          var properties = new Properties();
          try (var stream = source.toURL().openStream()) {
            var string = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            properties.load(new StringReader(string));
            var links =
                properties.stringPropertyNames().stream()
                    .filter(name -> !name.startsWith("@"))
                    .sorted()
                    .map(name -> Link.of(name, properties.getProperty(name)))
                    .toList();
            for (var link : links) {
              var name = link.name();
              if (pins.contains(link)) continue;
              if (map.containsKey(name)) {
                if (link.location().equals(map.get(name).location())) continue;
                duplicates.add(link);
              } else map.put(name, link);
            }
          } catch (Exception exception) {
            throw new RuntimeException("Loading lookup failed for: " + source, exception);
          }
        }
        if (!duplicates.isEmpty()) {
          throw new RuntimeException("Duplicates detected: " + duplicates);
        }
        if (!map.isEmpty()) lines.add("");
        for (var link : map.values()) {
          lines.add(link.name() + "=" + link.location());
        }
        try {
          Files.write(
              target,
              lines,
              StandardCharsets.UTF_8,
              StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception exception) {
          throw new RuntimeException("Write failed for: " + target, exception);
        }
      }
    }
  }
}
