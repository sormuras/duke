package test.jdk.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import jdk.tools.ToolRunner;
import org.junit.jupiter.api.Test;

class ToolRunnerTests {
  @Test
  void system() {
    var runner = ToolRunner.ofSystem();
    var finder = runner.context().finder();
    var javac = finder.find("javac").orElseThrow();
    assertEquals("jdk.compiler/javac", javac.toNamespaceAndName());
    assertThrows(NoSuchElementException.class, () -> finder.find("/").orElseThrow());
  }
}
