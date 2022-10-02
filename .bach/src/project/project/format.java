package project;

import java.util.List;
import run.bach.*;

public final class format implements Operator {
  @Override
  public void operate(Bach bach, List<String> arguments) {
    var name = "google-java-format@1.15.0";
    bach.run("load-tool", name);
    bach.run(name, format -> format.with("--replace").withFindFiles("**.java"));
  }
}
