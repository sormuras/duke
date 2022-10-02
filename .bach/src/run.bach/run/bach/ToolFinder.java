package run.bach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import run.bach.internal.PathSupport;

@FunctionalInterface
public interface ToolFinder {
  List<Tool> findAll();

  default Optional<Tool> findFirst(String string) {
    return findAll().stream().filter(tool -> tool.matches(string)).findFirst();
  }

  default String description() {
    return getClass().getSimpleName();
  }

  static ToolFinder of(String description, List<Tool> tools) {
    return new ListToolFinder(description, List.copyOf(tools));
  }

  static ToolFinder ofNativeToolInJavaHome(String tool, String... more) {
    var home = Path.of(System.getProperty("java.home"));
    var files = new ArrayList<String>();
    files.add(tool);
    files.addAll(List.of(more));
    var tools = new ArrayList<Tool>();
    for (var file : files) {
      var name = "${JAVA_HOME}/bin/" + file; // stable name, also on Windows
      var executable = home.resolve("bin").resolve(file).toString();
      tools.add(Tool.ofNativeProcess(name, List.of(executable)));
    }
    return ToolFinder.of("Native Tools in ${JAVA_HOME} -> " + home.toUri(), tools);
  }

  static ToolFinder ofJavaLauncherPrograms(Path directory) {
    var java = Path.of(System.getProperty("java.home"), "bin", "java");
    return ofJavaLauncherPrograms(directory, java);
  }

  static ToolFinder ofJavaLauncherPrograms(Path directory, Path java) {
    return new JavaProgramsToolFinder("Java Programs", directory, java);
  }

  record ListToolFinder(String description, List<Tool> findAll) implements ToolFinder {}

  record JavaProgramToolFinder(Path path, Path java) implements ToolFinder {
    @Override
    public List<Tool> findAll() {
      return findFirst(PathSupport.name(path, "?")).stream().toList();
    }

    @Override
    public Optional<Tool> findFirst(String name) {
      var directory = path.normalize().toAbsolutePath();
      if (!Files.isDirectory(directory)) return Optional.empty();
      var namespace = path.getParent().getFileName().toString();
      if (!name.equals(directory.getFileName().toString())) return Optional.empty();
      var command = new ArrayList<String>();
      command.add(java.toString());
      var args = directory.resolve("java.args");
      if (Files.isRegularFile(args)) {
        command.add("@" + args);
        return Optional.of(Tool.ofNativeProcess(namespace + '/' + name, command));
      }
      var jars = PathSupport.list(directory, PathSupport::isJarFile);
      if (jars.size() == 1) {
        command.add("-jar");
        command.add(jars.get(0).toString());
        return Optional.of(Tool.ofNativeProcess(namespace + '/' + name, command));
      }
      var javas = PathSupport.list(directory, PathSupport::isJavaFile);
      if (javas.size() == 1) {
        command.add(javas.get(0).toString());
        return Optional.of(Tool.ofNativeProcess(namespace + '/' + name, command));
      }
      return Optional.empty();
    }
  }

  record JavaProgramsToolFinder(String description, Path path, Path java) implements ToolFinder {
    @Override
    public List<Tool> findAll() {
      return PathSupport.list(path, Files::isDirectory).stream()
          .map(directory -> new JavaProgramToolFinder(directory, java))
          .map(ToolFinder::findAll)
          .flatMap(List::stream)
          .toList();
    }
  }
}
