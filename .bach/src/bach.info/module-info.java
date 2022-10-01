import bach.Bach.API.Operator;
import bach.Bach.Configuration.Creator;
import bach.info.ProjectLocalCreator;
import bach.info.ProjectLocalOperator;
import bach.info.ProjectLocalTool;
import java.util.spi.ToolProvider;

module bach.info {
  requires bach;

  provides Creator with
      ProjectLocalCreator;
  provides Operator with
      ProjectLocalOperator;
  provides ToolProvider with
      ProjectLocalTool;
}
