package run.bach;

import java.io.PrintWriter;
import java.util.StringJoiner;

public record Printer(PrintWriter out, PrintWriter err, System.Logger.Level threshold, int margin) {
  public void println(System.Logger.Level level, String text) {
    if (threshold == System.Logger.Level.OFF) {
      return;
    }
    var severity = level.getSeverity();
    if (severity >= System.Logger.Level.ERROR.getSeverity()) {
      err.println(text); // ignore margin
      return;
    }
    if (threshold != System.Logger.Level.ALL && severity < threshold.getSeverity()) {
      return;
    }
    if (text.length() <= margin) {
      out.println(text);
      return;
    }
    var lines = new StringJoiner("\n");
    for (var line : text.lines().toList()) {
      lines.add(line.length() <= margin ? line : line.substring(0, margin - 3) + "...");
    }
    out.println(lines);
  }
}
