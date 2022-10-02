import bach.Bach.API.*;
import bach.Bach.Browsing.*;
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
  uses Library;
  uses Operator;
  uses ToolProvider;

  provides Operator with
      LoadFileOperator,
      LoadHeadOperator,
      LoadMissingModulesOperator,
      LoadModuleOperator,
      LoadTextOperator,
      ListLibrariesOperator,
      ListPathsOperator,
      ListModulesOperator,
      ListToolsOperator;
  provides ToolProvider with
      ListFilesTool,
      TreeCreateTool,
      TreeDeleteTool,
      TreeTool;
}
