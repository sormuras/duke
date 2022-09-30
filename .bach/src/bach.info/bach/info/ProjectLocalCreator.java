package bach.info;

import bach.Bach;

public class ProjectLocalCreator implements Bach.Creator {
  @Override
  public Bach.API createBach(Bach.Configuration configuration) {
    return new ProjectLocalAPI(configuration);
  }
}
