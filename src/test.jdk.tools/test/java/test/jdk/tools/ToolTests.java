package test.jdk.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.spi.ToolProvider;
import jdk.tools.Tool;
import org.junit.jupiter.api.Test;

class ToolTests {
  @Test
  void factories() {
    var t1 = Tool.of("javac");
    var t2 = Tool.of(ToolProvider.findFirst("javac").orElseThrow());
    var t3 = Tool.of("jdk.compiler", ToolProvider.findFirst("javac").orElseThrow());

    assertEquals("jdk.compiler/javac", t1.toNamespaceAndName());
    assertEquals("jdk.compiler/javac", t2.toNamespaceAndName());
    assertEquals("jdk.compiler/javac", t3.toNamespaceAndName());
  }
}
