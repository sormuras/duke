package bach.info;

import bach.Bach.API.Locator;

public class JUnitLocator implements Locator {
  @Override
  public String description() {
    // @description The testing framework for Java and the JVM
    // @home https://junit.org
    // @version 5.9.1
    return "JUnit 5.9.1";
  }

  @Override
  public String locate(String name) {
    return switch (name) {
      case "org.apiguardian.api" -> "https://repo.maven.apache.org/maven2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar#SIZE=6806";
      case "org.junit.jupiter" -> "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter/5.9.1/junit-jupiter-5.9.1.jar#SIZE=6358";
      case "org.junit.jupiter.api" -> "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-api/5.9.1/junit-jupiter-api-5.9.1.jar#SIZE=207720";
      case "org.junit.jupiter.engine" -> "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.9.1/junit-jupiter-engine-5.9.1.jar#S=246530";
      case "org.junit.jupiter.migrationsupport" -> "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-migrationsupport/5.9.1/junit-jupiter-migratiupport-5.9.1.jar#SIZE=27687";
      case "org.junit.jupiter.params" -> "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-params/5.9.1/junit-jupiter-params-5.9.1.jar#SIZE=57894";
      case "org.junit.platform.commons" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-commons/1.9.1/junit-platform-commons-1.9.1.jar#SIZ02986";
      case "org.junit.platform.console" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console/1.9.1/junit-platform-console-1.9.1.jar#SIZE=51615";
      case "org.junit.platform.engine" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-engine/1.9.1/junit-platform-engine-1.9.1.jar#SIZE=18820";
      case "org.junit.platform.jfr" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-jfr/1.9.1/junit-platform-jfr-1.9.1.jar#SIZE=1910";
      case "org.junit.platform.launcher" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-launcher/1.9.1/junit-platform-launcher-1.9.1.jar#SIZE=169211";
      case "org.junit.platform.reporting" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-reporting/1.9.1/junit-platform-reporting-1.9.1.jar#SIZE=101";
      case "org.junit.platform.suite" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-suite/1.9.1/junit-platform-suite-1.9.1.jar#SIZE=636";
      case "org.junit.platform.suite.api" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-suite-api/1.9.1/junit-platform-suite-api-1.9.1.jar#SIZE=21227";
      case "org.junit.platform.suite.commons" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-suite-commons/1.9.1/junit-platform-suite-commons-1.9.1.jar#SIZE=15294";
      case "org.junit.platform.suite.engine" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-suite-engine/1.9.1/junit-platform-suite-engine-1.9.1.jar#SIZE=2426";
      case "org.junit.platform.testkit" -> "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-testkit/1.9.1/junit-platform-testkit-1.9.1.jar#SIZE=4439";
      case "org.junit.vintage.engine" -> "https://repo.maven.apache.org/maven2/org/junit/vintage/junit-vintage-engine/5.9.1/junit-vintage-engine-5.9.1.jar#SIZE=6689";
      case "org.opentest4j" -> "https://repo.maven.apache.org/maven2/org/opentest4j/opentest4j/1.2.0/opentest4j-1.2.0.jar#SIZE=7653";
      default -> null;
    };
  }
}
