package bach.info;

import bach.Bach.API;
import java.util.List;

public record ProjectLocalJarOperator(String name) implements API.Operator {
  public ProjectLocalJarOperator() {
    this("jar");
  }

  @Override
  public void operate(API bach, List<String> arguments) {
    bach.run("jdk.jartool/jar", arguments);
  }
}
