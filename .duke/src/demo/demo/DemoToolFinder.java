package demo;

import java.util.List;
import jdk.tools.Tool;
import jdk.tools.ToolFinder;

public class DemoToolFinder implements ToolFinder {
  @Override
  public List<Tool> tools() {
    return List.of(new DemoStatus("status"));
  }
}
