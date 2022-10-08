package project;

import java.util.List;
import run.bach.Bach;
import run.bach.BachOperator;

public class ProjectLocalOperator implements BachOperator {
  @Override
  public String name() {
    return "noop";
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {}
}
