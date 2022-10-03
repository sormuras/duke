package run.bach;

import java.io.PrintWriter;

public record Printer(System.Logger.Level threshold, PrintWriter out, PrintWriter err) {
  public void println(System.Logger.Level level, String text) {
    if (threshold == System.Logger.Level.OFF) {
      return;
    }
    var severity = level.getSeverity();
    if (severity >= System.Logger.Level.ERROR.getSeverity()) {
      err.println(text);
      return;
    }
    if (threshold != System.Logger.Level.ALL && severity < threshold.getSeverity()) {
      return;
    }
    out.println(text);
  }
}
