package test.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.duke.ToolOperator;
import run.duke.Workbench;

public record MockOperator() implements ToolOperator {
  @Override
  public String name() {
    return "moper";
  }

  @Override
  public ToolProvider provider(Workbench workbench) {
    return new Provider(name(), workbench);
  }

  record Provider(String name, Workbench workbench) implements ToolProvider {
    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      System.out.println(workbench.workpiece(String.class));
      for (var arg : args) workbench.run(arg);
      return 0;
    }
  }
}
