package run.bach.internal.tool;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;
import run.bach.Bach;
import run.bach.Configuration;
import run.bach.Operator;
import run.bach.internal.ExternalPropertiesStorage;

public record ListStorageOperator(String name) implements Operator {
  record CLI(Optional<Boolean> __help, Optional<String> storage) {
    CLI() {
      this(Optional.empty(), Optional.empty());
    }

    boolean help() {
      return __help.orElse(false);
    }

    CLI withParsingCommandLineArguments(List<String> args) {
      var arguments = new ArrayDeque<>(args);
      // extract
      var help = __help.orElse(null);
      var storage = this.storage.orElse(null);
      // handle
      while (!arguments.isEmpty()) {
        var argument = arguments.removeFirst();
        /* parse flags */ {
          if (Configuration.HELP_FLAGS.contains(argument)) {
            help = Boolean.TRUE;
            continue;
          }
        }
        /* parse key-value pairs */ {
          int sep = argument.indexOf('=');
          var key = sep == -1 ? argument : argument.substring(0, sep);
          if (key.equals("--storage")) {
            storage = sep == -1 ? arguments.removeFirst() : argument.substring(sep + 1);
            continue;
          }
        }
        /* treat first unhandled argument as storage */ {
          storage = argument;
          break;
        }
      }
      // compose
      return new CLI(Optional.ofNullable(help), Optional.ofNullable(storage));
    }
  }

  public ListStorageOperator() {
    this("list-storage");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    var cli = new CLI().withParsingCommandLineArguments(arguments);
    if (cli.help()) {
      bach.info("Usage: bach %s [?] [<storage>]".formatted(name()));
      return;
    }
    var renderer =
        cli.storage()
            .map(storage -> StorageInspector.of(bach, ExternalPropertiesStorage.of(storage)))
            .orElseGet(() -> StorageInspector.of(bach.paths().root()));
    bach.info(renderer.toString(0));
  }

  record StorageInspector(List<String> libraries, List<String> toolboxes) {
    static StorageInspector of(Path path) {
      var list = listDirectoryTree(path);
      return new StorageInspector(
          streamPropertiesNames(list, "/external-modules/").toList(),
          streamPropertiesNames(list, "/external-tools/").toList());
    }

    static StorageInspector of(Bach bach, ExternalPropertiesStorage storage) {
      try {
        var client = bach.browser().client();
        var request = HttpRequest.newBuilder(URI.create(storage.zip())).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 400) throw new RuntimeException(response.toString());
        var list = listZipInputStream(response.body());
        return new StorageInspector(
            streamPropertiesNames(list, "/external-modules/").toList(),
            streamPropertiesNames(list, "/external-tools/").toList());
      } catch (Exception exception) {
        throw (exception instanceof RuntimeException re) ? re : new RuntimeException(exception);
      }
    }

    public String toString(int indent) {
      var joiner = new StringJoiner("\n");

      var size = libraries.size();
      joiner.add("Libraries");
      libraries.forEach(name -> joiner.add("  " + name));
      joiner.add("    %d librar%s".formatted(size, size == 1 ? "y" : "ies"));

      size = toolboxes.size();
      joiner.add("Toolboxes");
      toolboxes.forEach(name -> joiner.add("  " + name));
      joiner.add("    %d toolbox%s".formatted(size, size == 1 ? "" : "es"));
      return joiner.toString().indent(indent).stripTrailing();
    }

    private static Stream<String> streamPropertiesNames(List<String> lines, String type) {
      return lines.stream()
          .filter(name -> name.contains(type))
          .map(name -> name.substring(name.lastIndexOf('/') + 1))
          .map(name -> name.replace(".properties", ""))
          .sorted();
    }

    private static List<String> listDirectoryTree(Path start) {
      if (!Files.isDirectory(start)) return List.of();
      var matcher = start.getFileSystem().getPathMatcher("glob:**.properties");
      try (var stream = Files.find(start, 99, (path, attributes) -> matcher.matches(path))) {
        return List.copyOf(stream.map(Path::toUri).map(URI::toString).toList());
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }

    private static List<String> listZipInputStream(InputStream stream) {
      var list = new ArrayList<String>();
      try (var zip = new ZipInputStream(stream)) {
        while (true) {
          var entry = zip.getNextEntry();
          if (entry == null) break;
          var name = entry.getName();
          if (!name.endsWith(".properties")) continue;
          list.add(name);
        }
        return List.copyOf(list);
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }
}
