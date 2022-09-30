package bach.info;

import bach.Bach;
import java.util.List;

public class ProjectLocalOperator implements Bach.Operator {
  @Override
  public String name() {
    return "operator";
  }

  @Override
  public void operate(Bach.API bach, List<String> arguments) {
    bach.info("OPERATOR!");
  }
}
