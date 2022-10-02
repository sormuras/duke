package run.bach.internal;

import java.util.Scanner;
import java.util.StringJoiner;

public interface ExternalPropertiesStorage {

  GitHub DEFAULT = new GitHub("sormuras", "bach-info", "HEAD");

  static ExternalPropertiesStorage of(String slug) {
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
    throw new RuntimeException("Storage not found: " + slug);
  }

  String properties(String type, String name);

  String zip();

  record GitHub(String user, String repo, String hash) implements ExternalPropertiesStorage {
    @Override
    public String properties(String type, String name) {
      return new StringJoiner("/", "https://github.com/", ".properties")
          .add(user)
          .add(repo)
          .add("raw")
          .add(hash)
          .add(".bach")
          .add(type)
          .add(name)
          .toString();
    }

    @Override
    public String zip() {
      return new StringJoiner("/", "https://github.com/", ".zip")
          .add(user)
          .add(repo)
          .add("archive")
          .add(hash)
          .toString();
    }
  }
}
