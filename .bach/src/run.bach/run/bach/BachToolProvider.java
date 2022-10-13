package run.bach;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record BachToolProvider(String name) implements ToolProvider {
  public BachToolProvider() {
    this("bach");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    return Bach.run(out, err, args);
  }
}
