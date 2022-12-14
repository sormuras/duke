module test.duke {
  requires run.duke;

  provides run.duke.ToolOperator with
      test.duke.MockOperator;
  provides java.util.spi.ToolProvider with
      test.duke.MockProvider;
}
