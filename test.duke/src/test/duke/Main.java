package test.duke;

import run.duke.Toolbox;

public class Main {
  public static void main(String... args) {
    var toolbox = Toolbox.compose(new MockToolbox(), Toolbox.ofSystem());
    toolbox.tools().forEach(System.out::println);
    var workbench = new MockWorkbench(toolbox);
    workbench.workpieces().put(String.class, "123");
    workbench.run("jar", "--version");
    workbench.run("moper", "mock0", "mock0", "mock0");
    workbench.run("jlink", "--version");
  }
}
