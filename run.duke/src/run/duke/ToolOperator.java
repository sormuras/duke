package run.duke;

import java.util.spi.ToolProvider;

public interface ToolOperator {
  String name();
  ToolProvider provider(ToolRunner runner);
}
