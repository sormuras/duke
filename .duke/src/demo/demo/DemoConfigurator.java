package demo;

import jdk.tools.ToolFinder;
import run.duke.Configuration;
import run.duke.Configurator;
import run.duke.DukeMenu;

public record DemoConfigurator() implements Configurator {
  @Override
  public Configuration configure(Configuration configuration) {
    var layer = configuration.layer();
    var finder = ToolFinder.compose(ToolFinder.of(layer), new DukeMenu());
    return configuration.with(finder);
  }
}
