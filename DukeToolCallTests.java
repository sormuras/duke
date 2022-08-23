public class DukeToolCallTests {
  public static void main(String... args) {
    testCommandLine();
  }

  static void testCommandLine() {
    var expected = "noop first-argument second-argument key value more README.md";
    var actual = Duke.ToolProgram.ToolCall.of("noop", "first-argument")
            .with("second-argument")
            .with("key", "value", "more")
            .withFindFiles("README.md")
            .toCommandLine();
    if (expected.equals(actual)) return;
    throw new AssertionError("Strings are not equal\n  Expected: " + expected + "\n    Actual: " + actual);
  }
}
