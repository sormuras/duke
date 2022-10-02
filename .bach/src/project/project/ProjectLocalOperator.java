package project;

import java.util.List;
import run.bach.Bach;
import run.bach.Operator;

public class ProjectLocalOperator implements Operator {
  @Override
  public String name() {
    return "noop";
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {}
}
