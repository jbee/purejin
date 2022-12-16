package project;

import java.nio.file.Path;
import java.util.List;
import run.bach.Composer;
import run.bach.Project;
import run.bach.Tweaks;

public class PurejinConfigurator extends Composer {
  @Override
  public Project createProject() {
    return new Project(
        new Project.Name("purejin"),
        new Project.Version("11-ea"),
        new Project.Spaces(
            new Project.Space(
                "main",
                11,
                "",
                declareModule("main", "se.jbee.inject"),
                declareModule("main", "se.jbee.inject.action"),
                declareModule("main", "se.jbee.inject.api"),
                declareModule("main", "se.jbee.inject.bind"),
                declareModule("main", "se.jbee.inject.bootstrap"),
                declareModule("main", "se.jbee.inject.container"),
                declareModule("main", "se.jbee.inject.contract"),
                declareModule("main", "se.jbee.inject.convert"),
                declareModule("main", "se.jbee.inject.event"),
                declareModule("main", "se.jbee.lang")),
            new Project.Space(
                "test",
                List.of("main"),
                0,
                List.of(),
                new Project.DeclaredModules(
                    declareModule("test", "se.jbee.junit.assertion"),
                    declareModule("test", "test.examples"),
                    declareModule("test", "test.integration")))),
        new Project.Externals());
  }

  private Project.DeclaredModule declareModule(String space, String module) {
    var content = Path.of(module);
    return new Project.DeclaredModule(content, content.resolve(space + "/java/module-info.java"));
  }

  @Override
  public Tweaks createTweaks() {
    return new Tweaks(
        call ->
            switch (call.name()) {
              case "javac" -> call.with("-g")
                  .with("-encoding", "UTF-8")
                  .with("-parameters")
                  .with("-Xlint:-missing-explicit-ctor,-serial");
              case "junit" -> call.with("--details", "NONE").with("--disable-banner");
              default -> call;
            });
  }
}
