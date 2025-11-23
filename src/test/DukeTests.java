import module java.base;

import org.junit.jupiter.api.*;

class DukeTests {
  @Test
  void test() {
    var missing = Duke.Modules.folder("lib").missing();
    Assertions.assertLinesMatch(List.of(), missing);
  }
}
