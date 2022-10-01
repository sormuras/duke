import bach.Bach;
import bach.Bach.API.FindTool;
import bach.Bach.API.ListOperator;
import bach.Bach.API.Operator;
import bach.Bach.Configuration.Creator;
import java.util.spi.ToolProvider;

module bach {
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jlink;
  requires jdk.jpackage;

  exports bach;

  uses Creator;
  uses Operator;
  uses ToolProvider;

  provides Operator with
      ListOperator;
  provides ToolProvider with
      Bach,
      FindTool;
}
