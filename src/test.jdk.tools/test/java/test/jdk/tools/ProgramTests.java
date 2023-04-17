package test.jdk.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.tools.Program;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProgramTests {
  @ParameterizedTest
  @ValueSource(strings = {"java", "javac", "javadoc", "jfr"})
  void findJavaDevelopmentKitTool(String name) {
    assertTrue(Program.findJavaDevelopmentKitTool(name).isPresent());
  }
}
