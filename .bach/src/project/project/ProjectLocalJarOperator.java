package project;

import java.util.List;
import run.bach.Bach;
import run.bach.Operator;

public record ProjectLocalJarOperator(String name) implements Operator {
  public ProjectLocalJarOperator() {
    this("jar");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    bach.run("jdk.jartool/jar", arguments);
  }
}
