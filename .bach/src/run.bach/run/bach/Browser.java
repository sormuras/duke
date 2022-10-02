package run.bach;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

/** A facade for a http client instance. */
public record Browser(HttpClient client) {
  public Browser() {
    this(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
  }

  public String read(URI source) {
    try {
      var request = HttpRequest.newBuilder(source).build();
      return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public HttpResponse<Path> load(URI source, Path target) {
    if (target.toString().isBlank()) throw new IllegalArgumentException("Blank target!");
    try {
      var parent = target.getParent();
      if (parent != null) Files.createDirectories(parent);
      var request = HttpRequest.newBuilder(source).build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));
      if (response.statusCode() >= 400) Files.deleteIfExists(target);
      return response;
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public HttpResponse<?> head(URI source) {
    try {
      var publisher = HttpRequest.BodyPublishers.noBody();
      var request = HttpRequest.newBuilder(source).method("HEAD", publisher).build();
      return client.send(request, HttpResponse.BodyHandlers.discarding());
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
