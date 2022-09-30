package bach.info;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class ProjectLocalTool implements ToolProvider {
  @Override
  public String name() {
    return "tool";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    out.println("TOOL!");
    return 0;
  }
}
