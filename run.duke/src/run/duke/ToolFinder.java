package run.duke;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;

public interface ToolFinder {
  List<? extends Tool> findTools();

  default Optional<? extends Tool> findTool(String tool) {
    return findTools().stream().filter(info -> info.test(tool)).findFirst();
  }

  static ToolFinder compose(ToolFinder... finders) {
    return new CompositeToolFinder(List.of(finders));
  }

  static ToolFinder ofSystem() {
    return new SystemToolFinder();
  }

  record CompositeToolFinder(List<ToolFinder> finders) implements ToolFinder {
    @Override
    public List<? extends Tool> findTools() {
      return finders.stream().flatMap(finder -> finder.findTools().stream()).toList();
    }
  }

  record SystemToolFinder() implements ToolFinder {
    @Override
    public List<? extends Tool> findTools() {
      return ServiceLoader.load(ToolProvider.class).stream()
          .map(ServiceLoader.Provider::get)
          .map(Tool::of)
          .toList();
    }
  }
}
