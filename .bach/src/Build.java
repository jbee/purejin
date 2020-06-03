/*
 *  Copyright (c) 2012-2020, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */

// default package

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.spi.ToolProvider;

/**
 * Silk's build program.
 */
class Build {
  public static void main(String... args) {
    var silk =
        Bach.Project.builder()
            .title("Silk DI")
            .version("1-ea")
            .setRealms(List.of(core(), example(), test()))
            .requires("org.hamcrest") // By junit at runtime.
            .requires("org.junit.vintage.engine") // Discovers and executes JUnit 3/4 tests.
            .requires("org.junit.platform.console") // Launch the JUnit Platform.
            .newProject();

    Bach.of(silk).build();

    var javadoc = ToolProvider.findFirst("javadoc").orElseThrow();
    javadoc.run(System.out, System.err,
        "--module", "se.jbee.inject",
        "--module-source-path", "se.jbee.inject=src/core;src/core-module",
        "-d", ".bach/workspace/documentation/api",
        "-encoding", "UTF-8",
        "-quiet",
        "-Xdoclint:none");
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
