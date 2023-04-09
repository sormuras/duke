package jdk.tools.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class DefaultToolTests {
  record TestToolProvider(String name) implements ToolProvider {
    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      return 0;
    }
  }

  @Test
  void components() {
    var provider = new TestToolProvider("test");
    var tool = new DefaultTool("tests", provider.name(), provider);

    assertEquals("tests", tool.namespace());
    assertEquals("test", tool.name());
    assertEquals("tests/test", tool.toNamespaceAndName());

    assertSame(provider, tool.provider());

    assertEquals(1, tool.tools().size());
    assertSame(tool, tool.tools().get(0));
  }
}
