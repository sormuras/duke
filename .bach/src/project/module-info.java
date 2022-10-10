import java.util.spi.*;
import project.*;
import run.bach.*;

module project {
  requires run.bach;

  provides Creator with
      ProjectLocalCreator;
  provides BachOperator with
      format,
      ProjectLocalOperator;
  provides ToolProvider with
      ProjectLocalTool;
}
