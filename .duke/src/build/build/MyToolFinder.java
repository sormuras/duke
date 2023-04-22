package build;

import java.util.List;
import jdk.tools.Task;
import jdk.tools.Tool;
import jdk.tools.ToolFinder;

public record MyToolFinder(String namespace) implements ToolFinder {
  public MyToolFinder() {
    this("my");
  }

  @Override
  public List<Tool> tools() {
    return List.of(
        new StatusToolOperator(namespace, "status"),
        Task.of(namespace, "versions", "jar --version + javac --version".split(" ")));
  }
}
