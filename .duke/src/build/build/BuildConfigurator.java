package build;

import run.duke.Configurator;
import run.duke.ToolFinder;

public record BuildConfigurator() implements Configurator {
  @Override
  public Configuration configure(Configuration configuration) {
    var finder =
        ToolFinder.compose(
            ToolFinder.of(new BuildToolOperator()),
            ToolFinder.of("javac", "jar", "javadoc"));
    return configuration.with(finder);
  }
}
