package test.duke;

import java.util.List;
import run.duke.Tool;
import run.duke.ToolFinder;

public class MockFinder implements ToolFinder {
  @Override
  public List<Tool> findTools() {
    return List.of(
        Tool.of(new MockProvider("mock0", 0)),
        Tool.of(new MockProvider("mock1", 1)),
        Tool.of(new MockProvider("mock2", 2)),
        Tool.of(new MockOperator("moper")));
  }
}
