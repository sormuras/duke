import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.util.stream.Stream;

public class DukeLogbookTests {
  public static void main(String... args) {
    printHi();
  }

  static void printHi() {
    var out = new StringWriter();
    var err = new StringWriter();

    class Greeter extends Duke.ToolProgram {
      Greeter() {
        super(
            new Logbook(Level.ALL, new PrintWriter(out), new PrintWriter(err)),
            new Browser());
      }

      void hi() {
        Stream.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARNING, Level.ERROR)
            .forEach(level -> logbook.log(level, level.name()));
        //noinspection resource
        logbook.out().println("Hi!");
      }
    }

    var greeter = new Greeter();
    greeter.hi();
    var actual = out.toString();
    if (!actual.contains("Hi!")) throw new AssertionError("Hi not found in:\n" + actual.indent(2));
  }
}
