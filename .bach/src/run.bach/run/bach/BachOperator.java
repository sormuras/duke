package run.bach;

import java.util.List;

@FunctionalInterface
public interface BachOperator {
  void operate(Bach bach, List<String> arguments) throws Exception;

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
