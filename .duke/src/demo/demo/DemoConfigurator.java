package demo;

import build.BuildToolOperator;
import run.duke.Configurator;
import run.duke.ToolFinder;

public record DemoConfigurator() implements Configurator {
  @Override
  public Configuration configure(Configuration configuration) {
    var finder =
        ToolFinder.compose(
            new BuildToolOperator(),
            new DemoToolFinder(),
            ToolFinder.of("javac", "jar", "javadoc"));
    return configuration.with(finder);
  }
}
