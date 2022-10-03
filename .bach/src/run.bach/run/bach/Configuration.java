package run.bach;

import java.util.StringJoiner;

public record Configuration(CLI cli, Printer printer) {
  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    joiner.add("Command-Line Interface Arguments");
    joiner.add(cli().toString(2));
    joiner.add("Components");
    joiner.add("  printer = " + printer);
    return joiner.toString().indent(indent).stripTrailing();
  }
}
