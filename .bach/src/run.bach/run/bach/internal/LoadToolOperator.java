package run.bach.internal;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import run.bach.Bach;
import run.bach.Configuration;
import run.bach.Operator;

public record LoadToolOperator(String name) implements Operator {

  record CLI(Optional<Boolean> __help, Optional<String> __from, List<String> names) {
    CLI() {
      this(Optional.empty(), Optional.empty(), List.of());
    }

    boolean help() {
      return __help.orElse(false);
    }

    ExternalPropertiesStorage fromExternalPropertiesStorage() {
      return __from.map(ExternalPropertiesStorage::of).orElse(ExternalPropertiesStorage.DEFAULT);
    }

    CLI withParsingCommandLineArguments(List<String> args) {
      var arguments = new ArrayDeque<>(args);
      var help = __help.orElse(null);
      var from = __from.orElse(null);
      var names = new ArrayList<>(names());
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
          if (key.equals("--from")) {
            from = sep == -1 ? arguments.removeFirst() : argument.substring(sep + 1);
            continue;
          }
        }
        // restore argument because first unhandled option marks the beginning of the library names
        arguments.addFirst(argument);
        break;
      }
      // parse variadic elements from remaining arguments
      names.addAll(arguments);
      // compose from components
      return new CLI(Optional.ofNullable(help), Optional.ofNullable(from), List.copyOf(names));
    }
  }

  public LoadToolOperator() {
    this("load-tool");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    var cli = new CLI().withParsingCommandLineArguments(arguments);
    if (cli.help()) {
      bach.info("Usage: bach %s [--from <store>] <tool-names...>".formatted(name()));
      return;
    }
    var directory = bach.paths().externalModules();
    var storage = cli.fromExternalPropertiesStorage();
    bach.debug("Load from %s".formatted(storage));
    for (var name : cli.names()) {
      var source = storage.properties("external-tools", name);
      var target = directory.resolve(name + ".properties");
      if (Files.notExists(target)) acquireProperties(bach, source, target);
      explodeProperties(bach, target);
    }
  }

  void acquireProperties(Bach bach, String source, Path target) {
    bach.info("load %s".formatted(source));
    var response = bach.browser().load(URI.create(source), target);
    if (Files.notExists(target)) throw new RuntimeException(response.toString());
  }

  void explodeProperties(Bach bach, Path file) {
    var properties = PathSupport.properties(file);
    var name = file.getFileName().toString();
    var parent = file.resolveSibling(name.substring(0, name.length() - 11));
    for (var key : properties.stringPropertyNames()) {
      if (key.startsWith("@")) continue;
      var source = URI.create(properties.getProperty(key));
      var target = parent.resolve(key);
      if (Files.exists(target)) continue;
      bach.info("load %s".formatted(source));
      var response = bach.browser().load(source, target);
      if (Files.notExists(target)) throw new RuntimeException(response.toString());
    }
  }
}
