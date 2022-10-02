package run.bach;

import java.util.List;
import java.util.Optional;

public interface ToolFinder {
  String description();

  List<Tool> findAll();

  default Optional<Tool> findFirst(String string) {
    return findAll().stream().filter(tool -> tool.matches(string)).findFirst();
  }

  static ToolFinder of(String description, List<Tool> tools) {
    record ListToolFinder(String description, List<Tool> findAll) implements ToolFinder {}
    return new ListToolFinder(description, List.copyOf(tools));
  }
}
