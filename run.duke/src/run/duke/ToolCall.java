package run.duke;

import java.util.List;

public record ToolCall(String tool, List<String> arguments) {}
