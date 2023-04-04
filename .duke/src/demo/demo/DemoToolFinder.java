package demo;

import java.io.PrintWriter;
import java.util.List;
import run.duke.Tool;
import run.duke.ToolCall;
import run.duke.ToolFinder;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public class DemoToolFinder implements ToolFinder {
  @Override
  public List<Tool> tools() {
    return List.of(status(), versions());
  }

  private Tool status() {
    record ListTools(String name) implements ToolOperator {
      @Override
      public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
        var tools = runner.finder().tools();
        for (var tool : tools) out.println(tool.toNamespaceAndName());
        out.printf("    %d tool%s%n", tools.size(), tools.size() == 1 ? "" : "s");
        return 0;
      }
    }
    return new ListTools("status");
  }

  private Tool versions() {
    return Tool.of(
        "demo",
        "versions",
        ToolCall.of("jar").with("--version"),
        ToolCall.of("javac").with("--version"),
        ToolCall.of("javadoc").with("--version"));
  }
}
