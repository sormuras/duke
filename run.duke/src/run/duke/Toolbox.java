package run.duke;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

@FunctionalInterface
public interface Toolbox {
  List<Tool> tools();

  default Optional<Tool> find(String tool) {
    return tools().stream().filter(info -> info.test(tool)).findFirst();
  }

  static Toolbox compose(Toolbox... toolboxes) {
    return new CompositeToolbox(List.of(toolboxes));
  }

  static Toolbox ofSystem() {
    return new SystemToolbox();
  }

  record CompositeToolbox(List<Toolbox> toolboxes) implements Toolbox {
    @Override
    public List<Tool> tools() {
      return toolboxes.stream().flatMap(toolbox -> toolbox.tools().stream()).toList();
    }
  }

  record SystemToolbox() implements Toolbox {
    @Override
    public List<Tool> tools() {
      var operators =
          ServiceLoader.load(ToolOperator.class).stream()
              .map(ServiceLoader.Provider::get)
              .map(Tool::of);
      var providers =
          ServiceLoader.load(ToolProvider.class).stream()
              .map(ServiceLoader.Provider::get)
              .map(Tool::of);
      return Stream.concat(operators, providers).toList();
    }
  }
}
