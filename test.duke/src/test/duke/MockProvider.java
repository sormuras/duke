package test.duke;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record MockProvider(String name, int code) implements ToolProvider {
  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.println("code = " + code);
    return code;
  }
}
