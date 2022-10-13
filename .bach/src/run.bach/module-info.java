import java.util.spi.*;
import run.bach.*;
import run.bach.internal.tool.*;

/** Defines Bach's API. */
module run.bach {
  requires transitive java.net.http;
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jlink;
  requires jdk.jpackage;

  exports run.bach;

  uses Creator;
  uses Library;
  uses BachOperator;
  uses ToolProvider;

  provides BachOperator with
      HashOperator,
      ImportOperator,
      InstallOperator,
      LoadFileOperator,
      LoadHeadOperator,
      LoadModuleOperator,
      LoadModulesOperator,
      LoadTextOperator,
      ListPathsOperator,
      ListModulesOperator,
      ListToolsOperator,
      ShowLogbookOperator,
      SignatureOperator;
  provides ToolProvider with
      BachToolProvider,
      ListFilesTool,
      TreeCreateTool,
      TreeDeleteTool,
      TreeTool;
}
