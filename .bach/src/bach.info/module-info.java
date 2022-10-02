import bach.Bach.API.*;
import bach.info.*;
import java.util.spi.ToolProvider;

module bach.info {
  requires bach;

  provides Creator with
      ProjectLocalCreator;
  provides Operator with
      ProjectLocalJarOperator,
      ProjectLocalOperator;
  provides ToolProvider with
      ProjectLocalTool;
}
