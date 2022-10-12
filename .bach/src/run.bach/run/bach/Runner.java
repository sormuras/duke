package run.bach;

import java.util.Optional;

/** A runner controls tool call runs. */
public record Runner(System.Logger.Level logLevel, Optional<Tool> tool) {
  public Runner(System.Logger.Level level) {
    this(level, Optional.empty());
  }

  public String logMessage(ToolCall call) {
    return "| %s".formatted(call.toCommandLine(" "));
  }
}
