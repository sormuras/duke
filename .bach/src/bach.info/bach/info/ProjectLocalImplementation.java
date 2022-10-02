package bach.info;

import bach.Bach.API.DefaultImplementation;
import bach.Bach.Configuration;

class ProjectLocalImplementation extends DefaultImplementation {
  ProjectLocalImplementation(Configuration configuration) {
    super(configuration);
  }

  @Override
  protected Browser createBrowser() {
    return super.createBrowser();
  }

  @Override
  protected Libraries createLibraries() {
    return super.createLibraries();
  }

  @Override
  protected Paths createPaths() {
    return super.createPaths();
  }

  @Override
  protected Toolbox createToolbox() {
    return super.createToolbox();
  }
}
