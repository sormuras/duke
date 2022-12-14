package run.duke;

import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;

public interface Workbench {
  default Optional<Tool> find(String tool) {
    return toolbox().find(tool);
  }

  default void run(String tool, String... args) {
    run(new ToolCall(tool, List.of(args)));
  }

  default void run(ToolCall call) {
    var tool = find(call.tool()).orElseThrow();
    var args = call.arguments().toArray(String[]::new);
    var provider = switchOverToolAndYieldToolProvider(tool, this);
    run(provider, args);
  }

  void run(ToolProvider provider, String... args);

  Toolbox toolbox();

  <T> T workpiece(Class<T> type);

  private static ToolProvider switchOverToolAndYieldToolProvider(Tool tool, Workbench workbench) {
    if (tool instanceof Tool.OfProvider of) return of.provider();
    if (tool instanceof Tool.OfOperator of) return of.operator().provider(workbench);
    throw new Error("Unsupported tool of " + tool.getClass());
  }
}
