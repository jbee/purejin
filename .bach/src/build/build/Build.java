package build;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Configuration;
import de.sormuras.bach.Flag;
import de.sormuras.bach.Project;
import de.sormuras.bach.project.MainSpace;
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
            .withMainSpaceUnit("src/se.jbee.inject/main/java-9", 8)
            .withMainSpaceUnit("src/se.jbee.inject.action/main/java-9", 8)
            .withMainSpaceUnit("src/se.jbee.inject.api/main/java-9", 8)
            .withMainSpaceUnit("src/se.jbee.inject.bind/main/java-9", 8)
            .withMainSpaceUnit("src/se.jbee.inject.bootstrap/main/java-9", 8)
            .withMainSpaceUnit("src/se.jbee.inject.container/main/java-9", 8)
            .withMainSpaceUnit("src/se.jbee.inject.convert/main/java-9", 8)
            .withMainSpaceUnit("src/se.jbee.inject.event/main/java-9", 8)

            .without(MainSpace.Modifier.CUSTOM_RUNTIME_IMAGE)
            .withMainSpaceJavacTweak(
                    javac -> javac.without("-Xlint").with("-Xlint:-serial,-rawtypes,-varargs")
              .without("--class-path").with("--class-path",
                                    ".bach/workspace/classes/11/se.jbee.inject.action" +
                                    ":.bach/workspace/classes/11/se.jbee.inject.api" +
                                    ":.bach/workspace/classes/11/se.jbee.inject.bind" +
                                    ":.bach/workspace/classes/11/se.jbee.inject.bootstrap" +
                                    ":.bach/workspace/classes/11/se.jbee.inject.container" +
                                    ":.bach/workspace/classes/11/se.jbee.inject.convert" +
                                    ":.bach/workspace/classes/11/se.jbee.inject.event")
            )

            .withMainSpaceJavadocTweak(
                    javadoc -> javadoc.without("-Xdoclint").with("-Xdoclint:all,-missing"))
            // test
            .withTestSpaceUnit("src/test.integration/test/java") // extra-module tests
            .withTestSpaceUnit("src/com.example.app/test/java") // silk's first client
            // lib/
            .withLibraryRequires(
                    "org.hamcrest", "org.junit.vintage.engine", "org.junit.platform.console");

    var configuration = Configuration.ofSystem().with(Level.INFO).with(Flag.SUMMARY_LINES_UNCUT);
    new Bach(configuration, silk).build();

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
