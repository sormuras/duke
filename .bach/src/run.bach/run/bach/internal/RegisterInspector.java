package run.bach.internal;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.zip.ZipInputStream;

public record RegisterInspector(Map<RegisterIndex, List<String>> map) {
  public static RegisterInspector of(Path path) {
    return new RegisterInspector(mapDirectoryTree(path));
  }

  public static RegisterInspector of(HttpClient client, Register repository) {
    try {
      var request = HttpRequest.newBuilder(URI.create(repository.zip())).build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() >= 400) throw new RuntimeException(response.toString());
      return new RegisterInspector(mapZipInputStream(response.body()));
    } catch (Exception exception) {
      throw (exception instanceof RuntimeException re) ? re : new RuntimeException(exception);
    }
  }

  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    for (var index : RegisterIndex.values()) {
      var list = map.get(index);
      if (list == null) {
        joiner.add(index + " not present");
        continue;
      }
      var size = list.size();
      joiner.add(index.toString());
      list.stream()
          .map(line -> line.substring(line.lastIndexOf('/') + 1))
          .map(line -> line.replace(index.extension(), ""))
          .sorted()
          .forEach(name -> joiner.add("  " + name));
      joiner.add("    %d element%s".formatted(size, size == 1 ? "" : "s"));
    }
    return joiner.toString().indent(indent).stripTrailing();
  }

  private static Map<RegisterIndex, List<String>> mapDirectoryTree(Path start) {
    if (!Files.isDirectory(start)) return Map.of();
    var map = new TreeMap<RegisterIndex, List<String>>();
    var matcher = start.getFileSystem().getPathMatcher("glob:**.properties");
    try (var stream = Files.find(start, 99, (path, attributes) -> matcher.matches(path))) {
      with_next_path:
      for (var path : stream.toList()) {
        var candidate = path.toUri().toString();
        for (var index : RegisterIndex.values()) {
          if (candidate.endsWith(index.extension())) {
            map.computeIfAbsent(index, key -> new ArrayList<>()).add(candidate);
            continue with_next_path;
          }
        }
      }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    return map;
  }

  private static Map<RegisterIndex, List<String>> mapZipInputStream(InputStream stream) {
    var map = new TreeMap<RegisterIndex, List<String>>();
    try (var zip = new ZipInputStream(stream)) {
      with_next_entry:
      while (true) {
        var entry = zip.getNextEntry();
        if (entry == null) break;
        var name = entry.getName();
        if (!name.endsWith(".properties")) continue;
        for (var index : RegisterIndex.values()) {
          if (name.endsWith(index.extension())) {
            map.computeIfAbsent(index, key -> new ArrayList<>()).add(name);
            continue with_next_entry;
          }
        }
      }
      return map;
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
