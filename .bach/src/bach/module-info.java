module bach {
  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jlink;
  requires jdk.jpackage;

  exports bach;

  uses bach.Bach.Creator;
  uses bach.Bach.Operator;
  uses java.util.spi.ToolProvider;

  provides bach.Bach.Operator with
      bach.Bach.Operators.ListOperator;
}
