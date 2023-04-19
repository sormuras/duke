import jdk.tools.Task;
import jdk.tools.ToolInstaller.Setup;

@Task.Of(
    name = "b",
    args = {"build"})
@Task.Of(
    name = "bb",
    args = {"border", "BEGIN", "+", "format", "+", "build", "+", "border", "END."})
@Setup(service = run.duke.store.GoogleJavaFormatInstaller.class, version = "1.16.0")
@Setup(service = run.duke.store.EchoInstaller.class, version = "99")
module build {
  requires border;
  requires jdk.tools;
  requires run.duke;
  requires run.duke.store;

  provides java.util.spi.ToolProvider with
      build.BuildToolOperator,
      build.FormatToolOperator;
}
