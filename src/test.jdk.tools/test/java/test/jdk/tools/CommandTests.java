package test.jdk.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import jdk.tools.Command;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CommandTests {
  @Test
  void createCommandWithoutArguments() {
    var command = Command.of("tool");
    assertEquals("tool", command.tool());
    assertEquals(List.of(), command.arguments());
    assertEquals("tool", command.toCommandLine());
    assertEquals("tool", command.toCommandLine("~"));
  }

  @Test
  void createCommandWithOneArgument() {
    var command = Command.of("tool", 1);
    assertEquals("tool", command.tool());
    assertEquals(List.of("1"), command.arguments());
    assertEquals("tool 1", command.toCommandLine());
    assertEquals("tool~1", command.toCommandLine("~"));
  }

  @Test
  void createCommandWithTwoArguments() {
    var command = Command.of("tool", 1, '2');
    assertEquals("tool", command.tool());
    assertEquals(List.of("1", "2"), command.arguments());
    assertEquals("tool 1 2", command.toCommandLine());
    assertEquals("tool~1~2", command.toCommandLine("~"));
  }

  @ParameterizedTest
  @CsvSource(
      textBlock =
          """
          tool, 0
          tool a, 1
          tool a b, 2
          tool a b c, 3
          """)
  void createCommandFromCommandLine(String line, int expectedSizeOfArguments) {
    var command = Command.ofCommandLine(line);
    assertEquals("tool", command.tool());
    assertEquals(expectedSizeOfArguments, command.arguments().size());
  }

  @Test
  void commandWithPositionalTweak() {
    var base = Command.of("tool", "(", ")");
    assertEquals("tool()", base.toCommandLine(""));
    assertEquals("tool|()", base.withTweak(0, tweak -> tweak.with("|")).toCommandLine(""));
    assertEquals("tool(|)", base.withTweak(1, tweak -> tweak.with("|")).toCommandLine(""));
    assertEquals("tool()|", base.withTweak(2, tweak -> tweak.with("|")).toCommandLine(""));
    assertEquals("tool()|", base.withTweak(tweak -> tweak.with("|")).toCommandLine(""));
  }

  @Test
  void commandWithTweaks() {
    var base = Command.of("tool", "(", ")");
    assertEquals("tool()", base.toCommandLine(""));
    assertEquals("tool()", base.withTweaks(List.of()).toCommandLine(""));
    assertEquals("tool()1", base.withTweaks(List.of(t -> t.with("1"))).toCommandLine(""));
    assertEquals(
        "tool[()12]",
        base.withTweaks(
                List.of(
                    t1 -> t1.with("1"),
                    t2 -> t2.with("2"),
                    t0 -> t0.withTweak(0, t -> t.with("[")),
                    t3 -> t3.with("]")))
            .toCommandLine(""));
  }
}
