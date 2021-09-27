import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.command.JavacCommand;
import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.project.ProjectSpace;
import com.github.sormuras.bach.project.ProjectSpaces;
import com.github.sormuras.bach.workflow.AbstractSpaceWorkflow;
import com.github.sormuras.bach.workflow.WorkflowBuilder;
import java.io.File;
import java.util.Set;

class build {
  public static void main(String... args) {
    var project =
        Project.of("purejin", "8.2-ea")
            .withSpaces(build::spaces)
            .withExternals(externals -> externals.withExternalModuleLocator(JUnit.version("5.8.1")))
            .with(Options.parse(args));
    var main = project.space("main");
    try (var bach = new PureBach()) {
      bach.logMessage("Build project %s".formatted(project.toNameAndVersion()));
      var builder = new WorkflowBuilder(bach, project);
      builder.grab();
      builder.compile();
      builder.runAllTests();
      builder.runWorkflow(new GenerateApiDocumentationWorkflow(bach, project, main));
    }
  }

  static ProjectSpaces spaces(ProjectSpaces spaces) {
    return spaces.withSpace("main", build::main).withSpace("test", Set.of("main"), build::test);
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withRelease(8)
        .withModule("se.jbee.inject/main/java-module/module-info.java")
        .withModule(
            "se.jbee.inject.action/main/java-module/module-info.java",
            module -> module.withSourcesFolder(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.api/main/java-module/module-info.java",
            module -> module.withSourcesFolder(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.bind/main/java-module/module-info.java",
            module -> module.withSourcesFolder(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.bootstrap/main/java-module/module-info.java",
            module -> module.withSourcesFolder(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.container/main/java-module/module-info.java",
            module -> module.withSourcesFolder(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.contract/main/java-module/module-info.java",
            module -> module.withSourcesFolder(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.convert/main/java-module/module-info.java",
            module -> module.withSourcesFolder(module.name() + "/main/java"))
        .withModule(
            "se.jbee.inject.event/main/java-module/module-info.java",
            module -> module.withSourcesFolder(module.name() + "/main/java"))
        .withModule(
            "se.jbee.lang/main/java-module/module-info.java",
            module -> module.withSourcesFolder(module.name() + "/main/java"));
  }

  static ProjectSpace test(ProjectSpace test) {
    return test.withModule("se.jbee.junit.assertion/test/java/module-info.java")
        .withModule(
            "test.examples/test/java/module-info.java",
            module -> module.withResourcesFolder(module.name() + "/test/resources"))
        .withModule(
            "test.integration/test/java/module-info.java",
            module -> module.withResourcesFolder(module.name() + "/test/resources"));
  }

  static class PureBach extends Bach {
    @Override
    public ToolRun run(Command<?> command) {
      if (command instanceof JavacCommand javac) {
        command = javac.add("-encoding", "UTF-8");
      }
      return super.run(command);
    }
  }

  static class GenerateApiDocumentationWorkflow extends AbstractSpaceWorkflow {

    GenerateApiDocumentationWorkflow(Bach bach, Project project, ProjectSpace space) {
      super(bach, project, space);
    }

    @Override
    public void run() {
      var api = bach.path().workspace("documentation", "api");
      bach.logCaption("Generate API documentation");
      bach.run(
          Command.javadoc()
              .add("--module", String.join(",", space.modules().names()))
              .add("-d", api)
              .add(
                  "--module-source-path",
                  "./*/main/java-module" + File.pathSeparator + "./*/main/java")
              .add("--module-path", ".bach/external-modules")
              .add("-notimestamp")
              .add("-encoding", "UTF-8")
              .add("-use")
              .add("-linksource")
              .add("-Xdoclint:-missing")
              .add(
                  "-group",
                  "API",
                  "se.jbee.inject:se.jbee.inject.api:se.jbee.inject.bind:se.jbee.inject.lang")
              .add("-group", "Container", "se.jbee.inject.bootstrap:se.jbee.inject.container")
              .add(
                  "-group",
                  "Add-ons",
                  "se.jbee.inject.action:se.jbee.inject.event:se.jbee.inject.convert:se.jbee.inject.contract"));
      bach.run(
          Command.jar()
              .mode("--create")
              .file(api.resolveSibling("api.jar"))
              .add("--no-manifest")
              .filesAdd(api));
    }
  }
}
