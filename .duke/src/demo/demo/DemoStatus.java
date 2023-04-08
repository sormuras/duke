package demo;

import java.io.PrintWriter;
import jdk.tools.ToolOperator;
import jdk.tools.ToolRunner;

record DemoStatus(String name) implements ToolOperator {
  @Override
  public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    var tools = runner.context().finder().tools();
    for (var tool : tools) out.println(tool.toNamespaceAndName());
    out.printf("    %d tool%s%n", tools.size(), tools.size() == 1 ? "" : "s");
    return 0;
  }
}
