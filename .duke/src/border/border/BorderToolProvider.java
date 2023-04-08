package border;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public record BorderToolProvider(String name) implements ToolProvider {
  public BorderToolProvider() {
    this("border");
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var caption = args.length == 0 ? "X" : String.join(" ", args);
    var border = "-".repeat(caption.length());
    out.printf("+-%s-+%n", border);
    out.printf("| %s |%n", caption);
    out.printf("+-%s-+%n", border);
    return 0;
  }

  public static void main(String... args) {
    new BorderToolProvider().run(System.out, System.err, args);
  }
}
