import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Call;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ProjectSpace;
import com.github.sormuras.bach.workflow.DeclaredModuleFinder;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

class build {
  public static void main(String... args) {
    var cli = Cli.of(args);
    var project =
        Project.of("purejin", cli.__project_version.orElse("8.2-ea"))
            .withMainSpace(build::main)
            .withTestSpace(build::test)
            .withExternalModuleLocators(JUnit.V_5_8_0_M1);

    Bach.build(new PureBach(project, Settings.of()));
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withJavaRelease(8)
        .withModuleSourcePaths("./*/main/java", "./*/main/java-module")
        .withModule("se.jbee.inject/main/java-module/module-info.java")
        .withModule("se.jbee.inject.action/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.api/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.bind/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.bootstrap/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.container/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.contract/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.convert/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.event/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.lang/main/java-module/module-info.java", build::main);
  }

  static DeclaredModule main(DeclaredModule module) {
    return module.withSources(module.name() + "/main/java");
  }

  static ProjectSpace test(ProjectSpace test) {
    return test.withModuleSourcePaths("./*/test/java")
        .withModule("se.jbee.junit.assertion/test/java/module-info.java")
        .withModule(
            "test.examples/test/java/module-info.java",
            module -> module.withResources("test.examples/test/resources"))
        .withModule(
            "test.integration/test/java/module-info.java",
            module -> module.withResources("test.integration/test/resources"))
        .withModulePaths(".bach/workspace/modules", ".bach/external-modules");
  }

  record Cli(Optional<String> __project_version, List<String> unhandled) {
    static Cli of(String... args) {
      String projectVersion = null;
      var todo = new LinkedList<>(List.of(args));
      var unhandled = new LinkedList<String>();
      while (!todo.isEmpty()) {
        switch (todo.peek()) {
          case "--project-version" -> {
            todo.pop();
            projectVersion = todo.pop();
          }
          default -> unhandled.add(todo.pop());
        }
      }
      return new Cli(Optional.ofNullable(projectVersion), List.copyOf(unhandled));
    }
  }

  static class PureBach extends Bach {

    public PureBach(Project project, Settings settings) {
      super(project, settings);
    }

    @Override
    public void executeTests() {
      super.executeTests();
      var main = project.spaces().main();
      var finder = DeclaredModuleFinder.of(main.modules());
      execute(
          Call.tool("javadoc")
              .with("--module", String.join(",", finder.names().toList()))
              .with("-d", folders.workspace("documentation", "api"))
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
}
