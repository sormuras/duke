module demo {
  requires build;
  requires run.duke;

  provides run.duke.Configurator with
      demo.DemoConfigurator;
  provides run.duke.ToolFinder with
      demo.DemoToolFinder;
}
