package run.bach.internal;

import java.util.Scanner;
import java.util.StringJoiner;

/** A repository for index files mapping external assets. */
public interface Register {

  GitHub DEFAULT = new GitHub("sormuras", "bach-register", "HEAD");

  static Register of(String slug) {
    if (slug == null || slug.isEmpty()) return DEFAULT;
    var scanner = new Scanner(slug);
    scanner.useDelimiter("/");
    var host = scanner.next(); // "github.com"
    if (host.equals("github.com")) {
      var user = scanner.hasNext() ? scanner.next() : DEFAULT.user();
      var repo = scanner.hasNext() ? scanner.next() : DEFAULT.repo();
      var hash = scanner.hasNext() ? String.join("/", scanner.tokens().toList()) : DEFAULT.hash();
      return new GitHub(user, repo, hash);
    }
    throw new RuntimeException("Repository slug not supported: " + slug);
  }

  String home();

  String source(Index index, String name);

  String zip();

  enum Index {
    /** A module-uri index mapping Java module names to their external modular JAR file locations. */
    LIBRARY_MODULES("external-modules", ".library.properties"),
    /** An asset-uri index mapping local file paths to their external resource locations. */
    TOOL_MATERIALS("external-tools", ".tool.properties");

    private final String directory;
    private final String extension;

    Index(String directory, String extension) {
      this.directory = directory;
      this.extension = extension;
    }

    public String directory() {
      return directory;
    }

    public String extension() {
      return extension;
    }

    public String name(String path) {
      return path.substring(path.lastIndexOf('/') + 1).replace(extension, "");
    }
  }

  record GitHub(String user, String repo, String hash) implements Register {
    @Override
    public String home() {
      var joiner = new StringJoiner("/", "https://github.com/", "");
      joiner.add(user).add(repo);
      if (!hash.equals(DEFAULT.hash)) joiner.add("tree").add(hash);
      return joiner.toString();
    }

    @Override
    public String source(Index index, String name) {
      var joiner = new StringJoiner("/", "https://github.com/", "");
      joiner.add(user).add(repo).add("raw").add(hash);
      joiner.add(".bach").add(index.directory()).add(name + index.extension());
      return joiner.toString();
    }

    @Override
    public String zip() {
      var joiner = new StringJoiner("/", "https://github.com/", ".zip");
      return joiner.add(user).add(repo).add("archive").add(hash).toString();
    }
  }
}
