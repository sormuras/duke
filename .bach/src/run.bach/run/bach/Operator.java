package run.bach;

import java.util.List;

@FunctionalInterface
public interface Operator {
  void operate(Bach bach, List<String> arguments);

  default String name() {
    return getClass().getSimpleName();
  }

  default boolean help(Bach bach, List<String> arguments, String options) {
    if (arguments.isEmpty() || CLI.isFirstArgumentHelpOptionName(arguments)) {
      bach.info("Usage: bach %s [?] %s".formatted(name(), options));
      return true;
    }
    return false;
  }
}
