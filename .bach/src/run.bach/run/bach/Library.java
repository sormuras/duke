package run.bach;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import run.bach.internal.PathSupport;

/** An external module library maps module names to their remote locations. */
@FunctionalInterface
public interface Library {
  default String locate(String name) {
    return locate(name, OperatingSystem.SYSTEM);
  }

  String locate(String name, OperatingSystem os);

  default String description() {
    return getClass().getSimpleName();
  }

  static Library ofProperties(Path file) {
    record PropertiesLibrary(String description, Properties properties) implements Library {
      @Override
      public String locate(String module, OperatingSystem os) {
        var key = module + '|' + os.name();
        {
          var location = properties.getProperty(key + '-' + os.architecture());
          if (location != null) return location;
        }
        {
          var location = properties.getProperty(key);
          if (location != null) return location;
        }
        return properties.getProperty(module);
      }
    }
    var properties = PathSupport.properties(file);
    var annotation =
        Optional.ofNullable(properties.remove("@description"))
            .map(Object::toString)
            .orElse(PathSupport.name(file, "?").replace(".properties", ""));
    var modules = new TreeSet<String>();
    for (var name : properties.stringPropertyNames()) {
      if (name.startsWith("@")) {
        properties.remove(name);
        continue;
      }
      var index = name.indexOf('|');
      modules.add(index <= 0 ? name : name.substring(0, index));
    }
    var description =
        "%s [%d/%d] %s".formatted(annotation, modules.size(), properties.size(), file.toUri());
    return new PropertiesLibrary(description, properties);
  }
}
