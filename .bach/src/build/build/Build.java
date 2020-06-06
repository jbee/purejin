package build;

import de.sormuras.bach.Bach;

class Build {
  public static void main(String... args) {
    var bach =
        Bach.of(
            scanner -> scanner.offset("src"),
            project ->
                project
                    .title("Silk DI")
                    .version("19.1-ea")
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
  }
}
