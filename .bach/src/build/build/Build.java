package build;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Configuration;
import de.sormuras.bach.Flag;
import de.sormuras.bach.Project;
import de.sormuras.bach.action.GenerateMavenPomFiles;
import de.sormuras.bach.project.CodeUnit;
import de.sormuras.bach.project.Feature;
import java.lang.System.Logger.Level;

/** purejin's build program. */
class Build {

  public static void main(String... args) {
    var version = "19.1-ea";

    var silk =
            Project.of()
                    .name("purejin")
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
      new GeneratePoms(bach).execute();
    });

  }

  static class GeneratePoms extends GenerateMavenPomFiles {

    public GeneratePoms(Bach bach) {
      super(bach, "    ");
    }

    @Override
    public String computeMavenGroupId(CodeUnit unit) {
      return "se.jbee.inject";
    }
  }

}
