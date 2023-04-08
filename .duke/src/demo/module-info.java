@jdk.tools.Task.Of(
    name = "versions",
    args = {"jar", "--version", "+", "javac", "--version", "+", "javadoc", "--version"})
module demo {
  requires jdk.tools;
  requires run.duke;

  provides run.duke.Configurator with
      demo.DemoConfigurator;
  provides jdk.tools.ToolFinder with
      demo.DemoToolFinder;
}
