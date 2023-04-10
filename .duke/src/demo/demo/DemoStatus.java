package demo;

import java.io.PrintWriter;
import jdk.tools.Command;
import jdk.tools.ToolOperator;
import jdk.tools.ToolRunner;

record DemoStatus(String name) implements ToolOperator {
  @Override
  public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    var tools = runner.context().finder().tools();
    for (var tool : tools) out.println(tool.toNamespaceAndName());
    out.printf("    %d tool%s%n", tools.size(), tools.size() == 1 ? "" : "s");

    var units = Command.of("?").withFindFiles("**/module-info.java").arguments();
    var size = units.size();
    if (size < 10) for (var unit : units) out.println(unit);
    else {
      for (var unit : units.subList(0, 3)) out.println(unit);
      System.out.println("[...]");
      for (var unit : units.subList(size - 3, size)) out.println(unit);
    }
    out.printf("    %d Java module compilation unit%s%n", size, size == 1 ? "" : "s");
    return 0;
  }
}
