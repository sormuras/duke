package build;

import jdk.tools.ToolFinder;
import run.duke.DukeInitializer;
import run.duke.store.GoogleJavaFormatInstaller;

public record BuildInitializer() implements DukeInitializer {
  @Override
  public ToolFinder initializeToolFinder(Helper helper) throws Exception {
    return helper.install(new GoogleJavaFormatInstaller(), "1.16.0");
  }
}
