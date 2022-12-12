package run.duke;

import java.util.List;
import java.util.spi.ToolProvider;

public interface ToolRunner {
  default void runTool(String tool, String... args) {
    runTool(new ToolCall(tool, List.of(args)));
  }

  default void runTool(ToolCall call) {
    var tool = toolFinder().findTool(call.tool()).orElseThrow();
    var provider = toolProvider(tool);
    var args = call.arguments().toArray(String[]::new);
    runTool(provider, args);
  }

  void runTool(ToolProvider provider, String... args);

  ToolFinder toolFinder();

  default ToolProvider toolProvider(Tool tool) {
    if (tool instanceof Tool.OfProvider of) return of.provider();
    if (tool instanceof Tool.OfOperator of) return of.operator().provider(this);
    throw new RuntimeException("Unsupported tool of " + tool.getClass());
  }
}
