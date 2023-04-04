package demo;

import build.BuildToolOperator;
import run.duke.Configuration;
import run.duke.Configurator;
import run.duke.ToolFinder;
import run.duke.menu.DukeMenu;

public record DemoConfigurator() implements Configurator {
  @Override
  public Configuration configure(Configuration configuration) {
    var finder =
        ToolFinder.compose(
            new BuildToolOperator(),
            new DemoToolFinder(),
            ToolFinder.of("javac", "jar", "javadoc"),
            new DukeMenu());
    return configuration.with(finder);
  }
}
