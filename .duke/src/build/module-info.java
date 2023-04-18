import jdk.tools.ToolInstaller.Setup;

@Setup(service = run.duke.store.GoogleJavaFormatInstaller.class, version = "1.16.0")
@Setup(service = run.duke.store.EchoInstaller.class, version = "99")
module build {
  requires jdk.tools;
  requires run.duke;
  requires run.duke.store;

  provides java.util.spi.ToolProvider with
      build.BuildToolOperator,
      build.FormatToolOperator;
}
