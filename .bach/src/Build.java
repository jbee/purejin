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

/** Silk's build program. */
class Build {
  public static void main(String... args) {
    var silk =
        Bach.Project.builder()
            .title("Silk DI")
            .version("1-ea")
            .setRealms(List.of(core()))
            .newProject();

    Bach.of(silk).build().assertSuccessful();
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
}
