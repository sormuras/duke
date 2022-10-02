package run.bach.internal;

import java.util.List;
import run.bach.Bach;
import run.bach.Operator;

public record ListPathsOperator(String name) implements Operator {
  public ListPathsOperator() {
    this("list-paths");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    bach.info(bach.paths().toString(0));
  }
}
