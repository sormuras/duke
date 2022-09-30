package bach.info;

import bach.Bach;

public class ProjectLocalCreator implements Bach.Creator {
  @Override
  public Bach.API create(Bach.Printer printer) {
    return new ProjectLocalAPI(printer);
  }
}
