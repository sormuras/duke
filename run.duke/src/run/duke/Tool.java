package run.duke;

import java.util.function.Predicate;
import java.util.spi.ToolProvider;

public sealed interface Tool extends Comparable<Tool>, Predicate<String> {
  String identifier();

  @Override
  default int compareTo(Tool other) {
    return identifier().compareTo(other.identifier());
  }

  @Override
  default boolean test(String tool) {
    return identifier().equals(tool) || identifier().endsWith('/' + tool);
  }

  static Tool of(ToolProvider provider) {
    var type = provider.getClass();
    var module = type.getModule();
    var namespace = module.isNamed() ? module.getName() : type.getPackageName();
    var nickname = provider.name();
    var identifier = namespace + '/' + nickname;
    return new OfProvider(identifier, provider);
  }

  static Tool of(ToolOperator operator) {
    var type = operator.getClass();
    var module = type.getModule();
    var namespace = module.isNamed() ? module.getName() : type.getPackageName();
    var nickname = operator.name();
    var identifier = namespace + '/' + nickname;
    return new OfOperator(identifier, operator);
  }

  record OfProvider(String identifier, ToolProvider provider) implements Tool {}

  record OfOperator(String identifier, ToolOperator operator) implements Tool {}
}
