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
  protected Locators createLocators() {
    return super.createLocators();
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
