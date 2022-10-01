module bach {
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jlink;
  requires jdk.jpackage;

  exports bach;

  uses bach.Bach.Configuration.Creator;
  uses bach.Bach.API.Operator;
  uses java.util.spi.ToolProvider;

  provides bach.Bach.API.Operator with
      bach.Bach.DefaultAPI.JarOperator,
      bach.Bach.DefaultAPI.ListOperator;
}
