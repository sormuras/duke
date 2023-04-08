package demo;

import java.io.PrintWriter;
import java.util.List;
import jdk.tools.Command;
import jdk.tools.Task;
import jdk.tools.Tool;
import jdk.tools.ToolFinder;
import jdk.tools.ToolOperator;
import jdk.tools.ToolRunner;

public class DemoToolFinder implements ToolFinder {
  @Override
  public List<Tool> tools() {
    return List.of(status(), versions());
  }

  private Tool status() {
    record ListTools(String name) implements ToolOperator {
      @Override
      public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
        var tools = runner.context().finder().tools();
        for (var tool : tools) out.println(tool.toNamespaceAndName());
        out.printf("    %d tool%s%n", tools.size(), tools.size() == 1 ? "" : "s");
        return 0;
      }
    }
    return new ListTools("status");
  }

  private Task versions() {
    return Task.of(
        "demo",
        "versions",
        Command.of("jar").with("--version"),
            Command.of("javac").with("--version"),
            Command.of("javadoc").with("--version"));
  }
}
