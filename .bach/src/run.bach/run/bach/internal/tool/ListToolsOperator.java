package run.bach.internal.tool;

import java.util.List;
import run.bach.Bach;
import run.bach.Operator;

public record ListToolsOperator(String name) implements Operator {
  public ListToolsOperator() {
    this("list-tools");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    bach.info(bach.tools().toString(0));
  }
}
