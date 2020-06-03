/*
 *  Copyright (c) 2012-2020, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */

// default package

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.spi.ToolProvider;

/** Silk's build program. */
class Build {
  public static void main(String... args) throws Exception {
    var version = "19.1-ea";
    var silk =
        Bach.Project.builder()
            .title("Silk DI")
            .version(version)
            .setRealms(List.of(core(), example(), test()))
            .requires("org.hamcrest") // By junit at runtime.
            .requires("org.junit.vintage.engine") // Discovers and executes JUnit 3/4 tests.
            .requires("org.junit.platform.console") // Launch the JUnit Platform.
            .newProject();

    Bach.of(silk).build();

    generateAndPackageApiDocumentation(version);
    generateMavenPomXml(version);
  }

  private static void generateAndPackageApiDocumentation(String version) {
    var javadoc = ToolProvider.findFirst("javadoc").orElseThrow();
    javadoc.run(
        System.out,
        System.err,
        "--module",
        "se.jbee.inject",
        "--module-source-path",
        "se.jbee.inject=src/core" + File.pathSeparator + "src/core-module",
        "-d",
        ".bach/workspace/documentation/api",
        "-encoding",
        "UTF-8",
        "-quiet",
        "-Xdoclint:none");

    var jar = ToolProvider.findFirst("jar").orElseThrow();
    jar.run(
        System.out,
        System.err,
        "--create",
        "--file",
        ".bach/workspace/documentation/silk-" + version + "-javadoc.jar",
        "--no-manifest",
        "-C",
        ".bach/workspace/documentation/api",
        ".");
  }

  private static void generateMavenPomXml(String version) throws Exception {
    Files.write(
        Path.of(".bach/workspace/se.jbee.inject@19.1-ea.pom.xml"),
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

  private static Bach.Project.Realm core() {
    var unit =
        new Bach.Project.Unit(
            Bach.Modules.describe(Path.of("src/core-module/module-info.java")),
            List.of(
                new Bach.Project.Source(Path.of("src/core"), 8),
                new Bach.Project.Source(Path.of("src/core-module"), 9)),
            List.of());

    return new Bach.Project.Realm(
        "core",
        Set.of(), // Set.of(Bach.Project.Realm.Flag.CREATE_API_DOCUMENTATION),
        Map.of(unit.descriptor().name(), unit),
        Set.of());
  }

  private static Bach.Project.Realm example() {
    var example =
        new Bach.Project.Unit(
            Bach.Modules.describe(Path.of("src/example-module/module-info.java")),
            List.of(
                new Bach.Project.Source(Path.of("src/example"), 0),
                new Bach.Project.Source(Path.of("src/example-module"), 0)),
            List.of());

    return new Bach.Project.Realm(
        "example",
        Set.of(Bach.Project.Realm.Flag.LAUNCH_TESTS),
        Map.of(example.descriptor().name(), example),
        Set.of("core"));
  }

  private static Bach.Project.Realm test() {
    var test =
        new Bach.Project.Unit(
            Bach.Modules.describe(Path.of("src/test-module/module-info.java")),
            List.of(
                new Bach.Project.Source(Path.of("src/test"), 0),
                new Bach.Project.Source(Path.of("src/test-module"), 0)),
            List.of());

    return new Bach.Project.Realm(
        "test",
        Set.of(Bach.Project.Realm.Flag.LAUNCH_TESTS),
        Map.of(test.descriptor().name(), test),
        Set.of("core"));
  }
}
