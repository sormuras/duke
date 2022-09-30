package bach.info;

import bach.Bach;

public class ProjectLocalOperator implements Bach.Operator {
  @Override
  public String name() {
    return "operator";
  }

  @Override
  public void operate(Bach.API bach, String... args) {
    bach.printer().out("OPERATOR!");
  }
}
