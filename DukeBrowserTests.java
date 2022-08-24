import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;

public class DukeBrowserTests {
  public static void main(String... args) throws Exception {
    var server = HttpServer.create(new InetSocketAddress("", 0), 0);
    var browser = new Duke.JavaProgram.Browser();
    try {
      server.start();
      testRead(server, browser);
      testLoad(server, browser);
      testHead(server, browser);
    } finally {
      server.stop(0);
    }
  }

  static void testRead(HttpServer server, Duke.JavaProgram.Browser browser) throws Exception {
    var address = server.getAddress();
    var source = URI.create("http://%s:%d".formatted(address.getHostString(), address.getPort()));
    var response = browser.read(source);
    if (response.equals("<h1>404 Not Found</h1>No context found for request")) return;
    System.err.println("Unexpected response: " + response);
  }

  static void testLoad(HttpServer server, Duke.JavaProgram.Browser browser) throws Exception {
    var address = server.getAddress();
    var source = URI.create("http://%s:%d".formatted(address.getHostString(), address.getPort()));
    var target = Path.of("load.file");
    var response = browser.load(source, target);
    if (response.statusCode() == 404) return;
    System.err.println("Unexpected response: " + response);
  }

  static void testHead(HttpServer server, Duke.JavaProgram.Browser browser) throws Exception {
    var address = server.getAddress();
    var source = URI.create("http://%s:%d".formatted(address.getHostString(), address.getPort()));
    var response = browser.head(source);
    if (response.statusCode() == 404) return;
    System.err.println("Unexpected response: " + response);
  }
}
