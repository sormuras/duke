package build;

import jdk.tools.Command;
import jdk.tools.Task;
import jdk.tools.ToolFinder;
import run.duke.DukeInitializer;
import run.duke.store.EchoInstaller;

public record MyInitializer(String namespace) implements DukeInitializer {
  public MyInitializer() {
    this("my");
  }

  @Override
  public ToolFinder initializeToolFinder(Helper helper) throws Exception {
    return ToolFinder.compose(
        helper.install(new EchoInstaller(), "99"),
        Task.of(namespace, "b", "build"),
        Task.of(
            namespace,
            "bb",
            Command.of("border", "BEGIN"),
            Command.of("format"),
            Command.of("b"),
            Command.of("border").with("END.")));
  }
}
