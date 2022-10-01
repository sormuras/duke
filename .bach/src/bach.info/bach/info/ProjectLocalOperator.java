package bach.info;

import bach.Bach.API;
import bach.Bach.Operator;

import java.util.List;

public class ProjectLocalOperator implements Operator {
  @Override
  public String name() {
    return "operator";
  }

  @Override
  public void operate(API bach, List<String> arguments) {
    bach.info("OPERATOR!");
  }
}
