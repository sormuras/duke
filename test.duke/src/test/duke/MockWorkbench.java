package test.duke;

import java.util.spi.ToolProvider;
import run.duke.Toolbox;
import run.duke.Workbench;
import run.duke.Workpieces;

public record MockWorkbench(Toolbox toolbox, Workpieces workpieces) implements Workbench {
  public MockWorkbench(Toolbox toolbox) {
    this(toolbox, new Workpieces());
  }

  @Override
  public void run(ToolProvider provider, String... args) {
    System.out.println("+ " + provider.name() + " " + String.join(" ", args));
    var code = provider.run(System.out, System.err, args);
    if (code != 0) throw new RuntimeException("Non-zero exit code: " + code);
  }

  @Override
  public <T> T workpiece(Class<T> type) {
    return workpieces.get(type);
  }
}
