@jdk.tools.Task.Of(
    namespace = "demo/task",
    name = "versions",
    args = {"jar", "--version", "+", "javac", "--version", "+", "javadoc", "--version"})
module demo {
  requires build;
  requires jdk.tools;
  requires run.duke;

  provides run.duke.Configurator with
      demo.DemoConfigurator;
  provides jdk.tools.ToolFinder with
      demo.DemoToolFinder;
}
