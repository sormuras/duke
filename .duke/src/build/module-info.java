module build {
  requires run.duke;

  provides java.util.spi.ToolProvider with
      build.BuildToolOperator;
  provides run.duke.Configurator with
      build.BuildConfigurator;
}
