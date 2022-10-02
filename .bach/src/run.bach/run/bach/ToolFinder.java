package run.bach;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface ToolFinder {
  List<Tool> findAll();

  default Optional<Tool> findFirst(String string) {
    return findAll().stream().filter(tool -> tool.matches(string)).findFirst();
  }

  default String description() {
    return getClass().getSimpleName();
  }

  static ToolFinder of(String description, List<Tool> tools) {
    return new ListToolFinder(description, List.copyOf(tools));
  }

  record ListToolFinder(String description, List<Tool> findAll) implements ToolFinder {}
}
