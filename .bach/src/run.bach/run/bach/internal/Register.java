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

  String source(RegisterIndex index, String name);

  String zip();

  record GitHub(String user, String repo, String hash) implements Register {
    @Override
    public String home() {
      var joiner = new StringJoiner("/", "https://github.com/", "");
      joiner.add(user).add(repo);
      if (!hash.equals(DEFAULT.hash)) joiner.add("tree").add(hash);
      return joiner.toString();
    }

    @Override
    public String source(RegisterIndex index, String name) {
      var joiner = new StringJoiner("/", "https://github.com/", "");
      joiner.add(user).add(repo).add("raw").add(hash);
      joiner.add(".bach").add(index.path()).add(name + index.extension());
      return joiner.toString();
    }

    @Override
    public String zip() {
      var joiner = new StringJoiner("/", "https://github.com/", ".zip");
      return joiner.add(user).add(repo).add("archive").add(hash).toString();
    }
  }
}
