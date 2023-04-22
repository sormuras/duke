module build {
  requires border;
  requires jdk.tools;
  requires run.duke;
  requires run.duke.store;

  provides jdk.tools.ToolFinder with
      build.MyToolFinder;
  provides run.duke.DukeInitializer with
      build.BuildInitializer,
      build.MyInitializer;
  provides java.util.spi.ToolProvider with
      build.BuildToolOperator,
      build.FormatToolOperator;
}
