module build {
  requires jdk.tools;

  exports build to demo;

  provides java.util.spi.ToolProvider with
      build.BuildToolOperator;
}
