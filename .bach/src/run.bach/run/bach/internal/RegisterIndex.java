package run.bach.internal;

public enum RegisterIndex {
  /** A module-uri index mapping Java module names to their remote modular JAR file locations. */
  EXTERNAL_MODULES_LOCATOR("external-modules", ".modules-locator.properties"),

  /** An asset-uri index mapping local file paths to their remote resource locations. */
  EXTERNAL_TOOL_DIRECTORY("external-tools", ".tool-directory.properties");

  private final String path;
  private final String extension;

  RegisterIndex(String path, String extension) {
    this.path = path;
    this.extension = extension;
  }

  public String path() {
    return path;
  }

  public String extension() {
    return extension;
  }

  public String name(String path) {
    return path.substring(path.lastIndexOf('/') + 1).replace(extension, "");
  }
}
