module build {
  requires jdk.tools;

  provides java.util.spi.ToolProvider with
      build.BuildToolOperator;
}
