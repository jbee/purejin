package build;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Configuration;
import de.sormuras.bach.Flag;
import de.sormuras.bach.Project;
import de.sormuras.bach.project.Feature;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Silk's build program. */
class Build {

  public static void main(String... args) throws Exception {
    var version = "19.1-ea";

    var silk =
            Project.of()
                    .name("silk")
                    .version(version)
                    // <main>
                    .module("src/se.jbee.inject/main/java-9", 8)
                    .module("src/se.jbee.inject.action/main/java-9", 8)
                    .module("src/se.jbee.inject.api/main/java-9", 8)
                    .module("src/se.jbee.inject.bind/main/java-9", 8)
                    .module("src/se.jbee.inject.bootstrap/main/java-9", 8)
                    .module("src/se.jbee.inject.container/main/java-9", 8)
                    .module("src/se.jbee.inject.convert/main/java-9", 8)
                    .module("src/se.jbee.inject.event/main/java-9", 8)
                    .module("src/se.jbee.inject.lang/main/java-9", 8)

                    .without(Feature.CREATE_CUSTOM_RUNTIME_IMAGE)
                    .tweakJavacCall(
                            javac -> javac.without("-Xlint").with("-Xlint:-serial,-rawtypes,-varargs"))

                    .tweakJavadocCall(
                            javadoc -> javadoc
                                .without("-Xdoclint")
                                .with("-Xdoclint:-missing")
                                .with("-group", "API", "se.jbee.inject:se.jbee.inject.api:se.jbee.inject.bind:se.jbee.inject.lang")
                                .with("-group", "Container", "se.jbee.inject.bootstrap:se.jbee.inject.container")
                                .with("-group", "Add-ons", "se.jbee.inject.action:se.jbee.inject.event:se.jbee.inject.convert")
                    )
                    // test
                    .withTestModule("src/test.integration/test/java") // extra-module tests
                    .withTestModule("src/com.example.app/test/java") // silk's first client
                    // lib/
                    .withLibraryRequires(
                            "org.hamcrest", "org.junit.vintage.engine", "org.junit.platform.console");

    var configuration = Configuration.ofSystem().with(Level.INFO).with(Flag.SUMMARY_LINES_UNCUT);
    new Bach(configuration, silk).build(bach -> {
      bach.deleteClassesDirectories();
      bach.executeDefaultBuildActions();
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
