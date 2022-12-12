package test.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public record MockOperator(String name) implements ToolOperator {
  @Override
  public ToolProvider provider(ToolRunner runner) {
    return new Provider(name(), (MockRunner) runner);
  }

  record Provider(String name, MockRunner runner) implements ToolProvider {
    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      for (var arg : args) runner.runTool(arg);
      return 0;
    }
  }
}
