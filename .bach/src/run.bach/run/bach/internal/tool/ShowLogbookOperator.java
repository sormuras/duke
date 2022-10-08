package run.bach.internal.tool;

import java.util.List;
import run.bach.Bach;
import run.bach.BachOperator;

public record ShowLogbookOperator(String name) implements BachOperator {
  public ShowLogbookOperator() {
    this("show-logbook");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) {
    bach.configuration().printer().out().println(bach.logbook().toMarkdown());
  }
}
