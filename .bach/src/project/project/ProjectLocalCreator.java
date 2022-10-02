package project;

import run.bach.Bach;
import run.bach.Configuration;
import run.bach.Creator;

public class ProjectLocalCreator implements Creator {
  @Override
  public Bach createBach(Configuration configuration) {
    return new ProjectLocalBach(configuration);
  }
}
