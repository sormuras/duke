module bach.info {
  requires bach;

  provides bach.Bach.Configuration.Creator with bach.info.ProjectLocalCreator;
  provides bach.Bach.Operator with bach.info.ProjectLocalOperator;
  provides java.util.spi.ToolProvider with bach.info.ProjectLocalTool;
}
