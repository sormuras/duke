import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import java.util.spi.ToolProvider;

interface bach {

  static void main(String... args) throws Exception {
    var sources = Path.of(".bach", "src");
    if (!Files.isDirectory(sources)) throw new RuntimeException("No sources found: " + sources);
    var classes = Path.of(".bach", "out", ".bach", "classes-" + Runtime.version().feature());
    var modules = modules(sources);
    run("javac", "--module=" + modules, "--module-source-path=" + sources, "-d", classes.toString());
    var process = new ProcessBuilder("java", "--module-path=" + classes);
    process.command().add("--module");
    process.command().add("bach/bach.Bach");
    process.command().addAll(List.of(args));
    var code = process.inheritIO().start().waitFor();
    if (code != 0) System.exit(code);
  }

  static String modules(Path sources) {
    try (var stream = Files.newDirectoryStream(sources, Files::isDirectory)) {
      var joiner = new StringJoiner(",");
      stream.forEach(dir -> joiner.add(dir.getFileName().toString()));
      return joiner.toString();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  static void run(String name, String... args) {
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var code = tool.run(System.out, System.err, args);
    if (code != 0) throw new RuntimeException(name + " -> " + code);
  }
}
