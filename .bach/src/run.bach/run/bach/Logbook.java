package run.bach;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/** A collector of log entries. */
public record Logbook(Deque<Log> logs) {
  public Logbook() {
    this(new ConcurrentLinkedDeque<>());
  }

  public void log(System.Logger.Level level, String text) {
    logs.add(new Log(Instant.now(), level, text));
  }

  public String toMarkdown() {
    var lines = new ArrayList<String>();
    lines.add("# Logbook");
    lines.add("");
    lines.add("## Log");
    for (var log : logs) {
      lines.add(log.instant() + " " + log.level());
      lines.add("```");
      log.text().lines().forEach(lines::add);
      lines.add("```");
    }
    return String.join("\n", lines);
  }

  public record Log(Instant instant, System.Logger.Level level, String text) {}
}
