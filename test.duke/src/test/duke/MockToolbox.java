package test.duke;

import java.util.List;
import run.duke.Tool;
import run.duke.Toolbox;

public class MockToolbox implements Toolbox {
  @Override
  public List<Tool> tools() {
    return List.of(Tool.of(new MockProvider("mock1", 1)), Tool.of(new MockProvider("mock2", 2)));
  }
}
