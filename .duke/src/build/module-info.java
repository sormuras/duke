module build {
  requires run.duke;

  exports build to demo;

  provides java.util.spi.ToolProvider with
      build.BuildToolOperator;
}
