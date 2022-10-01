package bach.info;

import bach.Bach.API;
import bach.Bach.Configuration;
import bach.Bach.API.Creator;

public class ProjectLocalCreator implements Creator {
  @Override
  public API createBach(Configuration configuration) {
    return new ProjectLocalImplementation(configuration);
  }
}
