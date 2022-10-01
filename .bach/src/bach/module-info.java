import bach.Bach;
import bach.Bach.API.FindTool;
import bach.Bach.API.ListOperator;
import bach.Bach.API.LoadFileOperator;
import bach.Bach.API.LoadHeadOperator;
import bach.Bach.API.LoadTextOperator;
import bach.Bach.API.Operator;
import bach.Bach.API.TreeTool;
import bach.Bach.Configuration.Creator;
import java.util.spi.ToolProvider;

/** Defines Bach's API. */
module bach {
  requires transitive java.net.http;
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
      LoadFileOperator,
      LoadHeadOperator,
      LoadTextOperator,
      ListOperator;
  provides ToolProvider with
      Bach,
      FindTool,
      TreeTool;
}
