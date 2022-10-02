package run.bach;

import java.util.List;
import java.util.StringJoiner;

public record Libraries(List<Library> list) {
  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    list.forEach(locator -> joiner.add(locator.description()));
    joiner.add("    %d librar%s".formatted(list.size(), list.size() == 1 ? "y" : "ies"));
    return joiner.toString().indent(indent).stripTrailing();
  }
}
