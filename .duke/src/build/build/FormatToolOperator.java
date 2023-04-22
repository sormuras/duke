package build;

import java.io.PrintWriter;
import java.util.Set;

import jdk.tools.Command;
import jdk.tools.ToolOperator;
import jdk.tools.ToolRunner;

public record FormatToolOperator(String name) implements ToolOperator {
  public FormatToolOperator() {
    this("format");
  }

  @Override
  public Set<String> requires() {
    return Set.of("google-java-format");
  }

  @Override
  public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    var format = Command.of("google-java-format").with("--replace").withFindFiles("**.java");
    runner.run(format);
    return 0;
  }
}
