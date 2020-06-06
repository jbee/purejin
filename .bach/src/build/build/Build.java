package build;

import de.sormuras.bach.Bach;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class Build {
  public static void main(String... args) throws Exception {
    var version = "19.1-ea";

    var bach =
        Bach.of(
            scanner -> scanner.offset("src"),
            project ->
                project
                    .title("Silk DI")
                    .version(version)
                    .requires("org.hamcrest") // By junit at runtime.
                    .requires("org.junit.vintage.engine") // Discovers and executes junit 3/4 tests.
                    .requires("org.junit.platform.console") // Launch the JUnit Platform.
            );

    bach.build(
        (arguments, project, context) -> {
          var tool = context.get("tool");
          if ("javadoc".equals(tool)) arguments.put("-Xdoclint:none");
          // if ("junit".equals(tool))
          //  arguments.put("--include-classname", "se.jbee.inject.bind.TestDiskScopeBinds");
        });

    generateMavenPomXml(version);
  }

  private static void generateMavenPomXml(String version) throws Exception {
    Files.write(
        Path.of(".bach/workspace/se.jbee.inject@" + version + ".pom.xml"),
        List.of(
            "<?xml version='1.0' encoding='UTF-8'?>",
            "<project xmlns='http://maven.apache.org/POM/4.0.0'",
            "    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'",
            "    xsi:schemaLocation='http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd'>",
            "    <modelVersion>4.0.0</modelVersion>",
            "",
            "    <groupId>se.jbee</groupId>",
            "    <artifactId>se.jbee.inject</artifactId>",
            "    <version>" + version + "</version>",
            "",
            "    <name>Silk DI</name>",
            "    <description>Silk Java dependency injection framework</description>",
            "",
            "    <url>http://www.silkdi.com</url>",
            "   <licenses>",
            "        <license>",
            "            <name>Apache License, Version 2.0</name>",
            "            <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>",
            "            <distribution>repo</distribution>",
            "        </license>",
            "    </licenses>",
            "    <scm>",
            "        <url>https://github.com/jbee/silk</url>",
            "        <connection>https://github.com/jbee/silk.git</connection>",
            "   </scm>",
            "",
            "    <developers>",
            "        <developer>",
            "            <id>jan</id>",
            "            <name>Jan Bernitt</name>",
            "            <email>jan@jbee.se</email>",
            "        </developer>",
            "    </developers>",
            "</project>"));
  }
}
