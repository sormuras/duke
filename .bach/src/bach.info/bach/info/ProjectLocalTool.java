package bach.info;

public class ProjectLocalTool implements java.util.spi.ToolProvider {
  @Override
  public String name() {
    return "tool";
  }

  @Override
  public int run(java.io.PrintWriter out, java.io.PrintWriter err, String... args) {
    out.println("TOOL!");
    return 0;
  }
}
