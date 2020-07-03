package build;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Builder;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class Build {

  public static void main(String... args) throws Exception {
    var version = "19.1-ea";

    Bach.of(
            project ->
                project
                    .withName("silk")
                    .withVersion(version)
                    .with(
                        Library.of()
                            .withRequires("org.hamcrest")
                            .withRequires("org.junit.vintage.engine")
                            .withRequires("org.junit.platform.console")))
        .with(System.Logger.Level.DEBUG)
        .with(CustomBuilder::new)
        .buildProject();

    generateMavenPomXml(version);
  }

  private static class CustomBuilder extends Builder {

    CustomBuilder(Bach bach) {
      super(bach);
    }

    @Override
    public Javac computeJavacForMainSources() {
      return super.computeJavacForMainSources().with("-Xlint:-serial,-rawtypes,-varargs");
    }

    @Override
    public Javadoc computeJavadocForMainSources() {
      return super.computeJavadocForMainSources().without("-Xdoclint").with("-Xdoclint:none");
    }
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
