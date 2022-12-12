package test.duke;

import run.duke.ToolFinder;

public class Main {
  public static void main(String... args) {
    var finder = ToolFinder.compose(new MockFinder(), ToolFinder.ofSystem());
    var runner = new MockRunner(finder);
    runner.runTool("jar", "--version");
    runner.runTool("moper", "mock0", "mock0", "mock0");
    runner.runTool("jlink", "--version");
  }
}
