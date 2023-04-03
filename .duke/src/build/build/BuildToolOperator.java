package build;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;
import run.duke.ToolCall;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

public record BuildToolOperator(String name) implements ToolOperator {
  private static final int RELEASE = 17;
  private static final Path CLASSES = Path.of(".duke/tmp/build/classes-" + RELEASE);
  private static final Path SOURCES = Path.of("src", "run.duke", "main", "java");

  public BuildToolOperator() {
    this("build");
  }

  @Override
  public int run(ToolRunner runner, PrintWriter out, PrintWriter err, String... args) {
    Consumer<ToolCall> run =
        (call) -> {
          var thread = Thread.currentThread().threadId();
          var command = call.toCommandLine();
          out.printf("[%04X] %s%n", thread, command);
          runner.run(call);
        };

    try {
      out.println("Processing source files...");
      Stream.of(compileJavaClasses(), generateHtmlPages()).parallel().forEach(run);

      out.println("Archiving class files...");
      run.accept(createJavaArchive());
      return 0;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }

  private ToolCall compileJavaClasses() {
    return ToolCall.of("javac")
        .with("--module", "run.duke")
        .with("--module-source-path", SOURCES.toString().replace("run.duke", "*"))
        .with("--release", RELEASE)
        .with("-W" + "error")
        .with("-X" + "lint")
        .with("-d", CLASSES);
  }

  private ToolCall createJavaArchive() {
    var file = Path.of(".duke/out/archives/main/run.duke.jar");
    return ToolCall.of("jar")
        .with("--create")
        .with("--file", file)
        .with("--main-class", "run.duke.Main")
        // TODO .with("--module-version", "0-ea")
        .with("-C", CLASSES.resolve("run.duke"), ".");
  }

  private ToolCall generateHtmlPages() {
    var destination = Path.of(".duke/out/documentation/api");
    return ToolCall.of("javadoc")
        .with("-quiet")
        .with("--module", "run.duke")
        .with("--module-source-path", SOURCES.toString().replace("run.duke", "*"))
        .with("-X" + "doc" + "lint:-missing")
        .with("-d", destination);
  }
}