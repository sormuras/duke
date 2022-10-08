package run.bach.internal.tool;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import run.bach.Bach;
import run.bach.BachOperator;

public record LoadFileOperator(String name) implements BachOperator {
  public LoadFileOperator() {
    this("load-file");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    if (help(bach, arguments, "<from-uri> [<to-path>]")) return;
    var uri = URI.create(arguments.get(0));
    var path = Path.of(arguments.get(1));
    var response = bach.browser().load(uri, path);
    if (Files.notExists(response.body())) throw new RuntimeException(response.toString());
  }
}
