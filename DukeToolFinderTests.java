public class DukeToolFinderTests extends Duke.ToolProgram {
  public static void main(String... args) {
    var duke = new DukeToolFinderTests();
    duke.versions();
  }

  DukeToolFinderTests() {
    super(
        new Logbook(System.Logger.Level.ALL),
        new Browser(),
        ToolFinder.compose(ToolFinder.ofSystemTools(), ToolFinder.ofNativeToolsInJavaHome("java")));
  }

  void versions() {
    finder.findAll().stream()
        .map(Tool::name)
        .sorted()
        .map(name -> ToolCall.of(name, (name.equals("jdk.jdeps/javap") ? "-" : "--") + "version"))
        .forEach(this::run);
  }
}
