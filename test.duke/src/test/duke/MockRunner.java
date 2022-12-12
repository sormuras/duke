package test.duke;

import java.util.spi.ToolProvider;
import run.duke.ToolFinder;
import run.duke.ToolRunner;

public record MockRunner(ToolFinder toolFinder) implements ToolRunner {
  @Override
  public void runTool(ToolProvider provider, String... args) {
    System.out.println("+ " + provider.name() + " " + String.join(" ", args));
    var code = provider.run(System.out, System.err, args);
    if (code != 0) throw new RuntimeException("Non-zero exit code: " + code);
  }
}
