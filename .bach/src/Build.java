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
  public static void main(String... args0) {
    Bach.of(
            scanner -> scanner.offset("src"),
            silk ->
                silk.title("Silk DI")
                    .version("19.1-ea")
                    .requires("org.hamcrest") // By junit at runtime.
                    .requires("org.junit.vintage.engine") // Discovers and executes junit 3/4 tests.
                    .requires("org.junit.platform.console") // Launch the JUnit Platform.
            )
        .build(
            (args, project, context) -> {
              if (context.get("tool").equals("javadoc")) args.put("-Xdoclint:none");
            });
  }
}
