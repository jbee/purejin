import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.Checkpoint;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.Tweak;
import com.github.sormuras.bach.call.JavacCall;
import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.project.ProjectSpace;
import com.github.sormuras.bach.workflow.BuildWorkflow;
import com.github.sormuras.bach.workflow.DeclaredModuleFinder;
import java.io.File;

class build {
  public static void main(String... args) {
    System.setProperty("java.util.logging.config.file", ".bach/src/logging.properties");
    var options = Options.of(args);
    var project =
        Project.of("purejin", "8.2-ea")
            .withMainSpace(build::main)
            .withTestSpace(build::test)
            .withExternalModuleLocators(JUnit.V_5_8_0_M1)
            .with(options);
    var settings =
        Settings.of()
            .withWorkflowTweakHandler(build::tweak)
            .withWorkflowCheckpointHandler(build::at)
            .with(options);
    Bach.build(new Bach(project, settings));
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withJavaRelease(8)
        .withModuleSourcePaths("./*/main/java", "./*/main/java-module")
        .withModule("se.jbee.inject/main/java-module/module-info.java")
        .withModule(
            "se.jbee.inject.action/main/java-module/module-info.java",
            module -> module.withSources(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.api/main/java-module/module-info.java",
            module -> module.withSources(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.bind/main/java-module/module-info.java",
            module -> module.withSources(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.bootstrap/main/java-module/module-info.java",
            module -> module.withSources(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.container/main/java-module/module-info.java",
            module -> module.withSources(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.contract/main/java-module/module-info.java",
            module -> module.withSources(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.convert/main/java-module/module-info.java",
            module -> module.withSources(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.event/main/java-module/module-info.java",
            module -> module.withSources(module.name() + "/main/java"))
        .withModule(
            "se.jbee.lang/main/java-module/module-info.java",
            module -> module.withSources(module.name() + "/main/java"));
  }

  static ProjectSpace test(ProjectSpace test) {
    return test.withModuleSourcePaths("./*/test/java")
        .withModule("se.jbee.junit.assertion/test/java/module-info.java")
        .withModule(
            "test.examples/test/java/module-info.java",
            module -> module.withResources(module.name() + "/test/resources"))
        .withModule(
            "test.integration/test/java/module-info.java",
            module -> module.withResources(module.name() + "/test/resources"))
        .withModulePaths(".bach/workspace/modules", ".bach/external-modules");
  }

  static Call tweak(Tweak tweak) {
    if (tweak.call() instanceof JavacCall call) {
      return call.with("-Xlint");
    }
    return tweak.call();
  }

  static void at(Checkpoint checkpoint) {
    if (checkpoint instanceof BuildWorkflow.SuccessCheckpoint) {
      generateApiDocumentation(checkpoint.workflow().bach());
    }
  }

  static void generateApiDocumentation(Bach bach) {
    var project = bach.project();
    var main = project.spaces().main();
    var finder = DeclaredModuleFinder.of(main.modules());
    bach.execute(
        Call.tool("javadoc")
            .with("--module", String.join(",", finder.names().toList()))
            .with("-d", bach.folders().workspace("documentation", "api"))
            .with(
                "--module-source-path",
                "./*/main/java-module" + File.pathSeparator + "./*/main/java")
            .with("--module-path", ".bach/external-modules")
            .with("-notimestamp")
            .with("-encoding", project.defaults().encoding())
            .with("-use")
            .with("-linksource")
            .with("-Xdoclint:-missing")
            .with(
                "-group",
                "API",
                "se.jbee.inject:se.jbee.inject.api:se.jbee.inject.bind:se.jbee.inject.lang")
            .with("-group", "Container", "se.jbee.inject.bootstrap:se.jbee.inject.container")
            .with(
                "-group",
                "Add-ons",
                "se.jbee.inject.action:se.jbee.inject.event:se.jbee.inject.convert:se.jbee.inject.contract"));
  }
}
