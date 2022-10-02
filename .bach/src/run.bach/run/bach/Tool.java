package run.bach;

import java.util.List;
import java.util.spi.ToolProvider;
import run.bach.internal.NativeProcessToolProvider;

public sealed interface Tool {
  String name();

  default String nick() {
    if (name().endsWith("/")) throw new IllegalStateException(name());
    return name().substring(name().lastIndexOf('/') + 1);
  }

  default boolean matches(String string) {
    return name().equals(string) || name().endsWith('/' + string);
  }

  void run(Bach bach, List<String> arguments);

  static Tool of(Operator operator) {
    return new BachOperatorTool(prefixIfNeeded(operator.name(), operator), operator);
  }

  static Tool of(ToolProvider provider) {
    return new ToolProviderTool(prefixIfNeeded(provider.name(), provider), provider);
  }

  static Tool ofNativeProcess(String name, List<String> command) {
    return Tool.of(new NativeProcessToolProvider(name, command));
  }

  private static String prefixIfNeeded(String name, Object object) {
    if (name.indexOf('/') >= 0) return name;
    var module = object.getClass().getModule();
    var prefix = module.isNamed() ? module.getName() : object.getClass().getCanonicalName();
    return prefix + '/' + name;
  }

  record ToolProviderTool(String name, ToolProvider provider) implements Tool {
    @Override
    public void run(Bach bach, List<String> arguments) {
      var printer = bach.configuration().printer();
      Thread.currentThread().setContextClassLoader(provider.getClass().getClassLoader());
      provider.run(printer.out(), printer.err(), arguments.toArray(String[]::new));
    }
  }

  record BachOperatorTool(String name, Operator operator) implements Tool {
    @Override
    public void run(Bach bach, List<String> arguments) {
      operator.operate(bach, arguments);
    }
  }
}
