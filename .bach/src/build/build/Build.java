package build;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Builder;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.MainSources;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.SourceDirectories;
import de.sormuras.bach.project.SourceDirectory;
import de.sormuras.bach.project.SourceUnit;
import de.sormuras.bach.project.SourceUnits;
import de.sormuras.bach.project.Sources;
import de.sormuras.bach.tool.Javac;
import de.sormuras.bach.tool.Javadoc;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Silk's build program. */
class Build {

  public static void main(String... args) throws Exception {
    var version = "19.1-ea";

    var unit =
        new SourceUnit(
            ModuleDescriptor.newModule("se.jbee.inject").build(),
            new SourceDirectories(
                List.of(
                    new SourceDirectory(Path.of("src/se.jbee.inject/main/java"), 8),
                    SourceDirectory.of(Path.of("src/se.jbee.inject/main/java-9")))),
            List.of());

    var bach =
        Bach.ofSystem()
            .with(System.Logger.Level.INFO)
            .with(
                Project.of("silk", version)
                    .with(Sources.of().with(MainSources.of().with(SourceUnits.of().with(unit))))
                    .withTestSource("src/se.jbee.inject/test/java-module") // in-module tests
                    .withTestSource("src/com.example.app/test/java") // silk's first client
                    .withTestSource("src/com.example.test/test/java") // modular integration tests
                    .with(
                        Library.of()
                            .withRequires("org.hamcrest")
                            .withRequires("org.junit.vintage.engine")
                            .withRequires("org.junit.platform.console")))
            .with(CustomBuilder::new);

    bach.buildProject();

    generateMavenPomXml(version);
  }

  private static class CustomBuilder extends Builder {

    CustomBuilder(Bach bach) {
      super(bach);
    }

    @Override
    public Javac computeJavacForMainSources() {
      return super.computeJavacForMainSources()
          .without("-Xlint")
          .with("-Xlint:-serial,-rawtypes,-varargs");
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
