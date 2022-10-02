package run.bach.internal;

import java.util.List;
import run.bach.Bach;
import run.bach.Operator;

public record ListLibrariesOperator(String name) implements Operator {
  public ListLibrariesOperator() {
    this("list-libraries");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    bach.info(bach.libraries().toString(0));
  }
}
