package demo;

import build.BuildToolOperator;
import jdk.tools.ToolFinder;
import run.duke.Configuration;
import run.duke.Configurator;
import run.duke.DukeMenu;

public record DemoConfigurator() implements Configurator {
  @Override
  public Configuration configure(Configuration configuration) {
    var finder =
        ToolFinder.compose(
            ToolFinder.of(configuration.layer()),
            new BuildToolOperator(),
            new DemoToolFinder(),
            ToolFinder.of("javac", "jar", "javadoc"),
            new DukeMenu());
    return configuration.with(finder);
  }
}
