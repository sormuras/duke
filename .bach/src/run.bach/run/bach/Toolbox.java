package run.bach;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeMap;

public record Toolbox(List<ToolFinder> finders) {
  public Tool get(String string) {
    for (var finder : finders) {
      var found = finder.findFirst(string);
      if (found.isEmpty()) continue;
      return found.get();
    }
    throw new UnsupportedOperationException(string);
  }

  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    var width = 3;
    var nicks = new TreeMap<String, List<Tool>>();
    for (var finder : finders) {
      for (var tool : finder.findAll()) {
        nicks.computeIfAbsent(tool.nick(), __ -> new ArrayList<>()).add(tool);
        var length = tool.nick().length();
        if (length > width) width = length;
      }
    }
    var format = "%" + width + "s %s";
    for (var entry : nicks.entrySet()) {
      var names = entry.getValue().stream().map(Tool::name).toList();
      joiner.add(String.format(format, entry.getKey(), names));
    }
    return joiner.toString().indent(indent).stripTrailing();
  }
}
