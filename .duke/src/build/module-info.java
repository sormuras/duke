module build {
  requires run.duke;

  provides java.util.spi.ToolProvider with
      build.BuildToolOperator;
}
