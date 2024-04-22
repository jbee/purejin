import java.nio.file.Path;
import java.util.List;
import run.bach.Bach;
import run.bach.ToolCall;
import run.bach.ToolRunner;
import run.bach.workflow.Builder;
import run.bach.workflow.Structure;
import run.bach.workflow.Structure.*;
import run.bach.workflow.Workflow;

record Project(Workflow workflow) implements Builder {
  static Project ofCurrentWorkingDirectory() {
    var basics = new Basics("purejin", "11-ea");
    var main =
        new Space(
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
            declareModule("main", "se.jbee.lang"));
    var test =
        new Space(
            "test",
            List.of(main.name()),
            0,
            List.of(),
            new DeclaredModules(
                declareModule("test", "se.jbee.junit.assertion"),
                declareModule("test", "test.examples"),
                declareModule("test", "test.integration")));

    return new Project(
        new Workflow(
            Bach.Folders.ofCurrentWorkingDirectory(),
            new Structure(basics, new Spaces(main, test)),
            ToolRunner.ofSystem()));
  }

  private static DeclaredModule declareModule(String space, String module) {
    var content = Path.of(module);
    return new DeclaredModule(content, content.resolve(space + "/java/module-info.java"));
  }

  @Override
  public void build() {
    Builder.super.build();
    // TODO document() by default?
  }

  @Override
  public ToolCall classesCompilerNewJavacToolCall() {
    return ToolCall.of("javac")
        .add("-g")
        .add("-encoding", "UTF-8")
        .add("-parameters")
        .add("-X" + "lint:-missing-explicit-ctor,-serial");
  }

  @Override
  public void testerRunJUnitPlatform(ToolCall junit) {
    run(junit.add("--details", "NONE").add("--disable-banner"));
  }

  void document() {
    var main = workflow.structure().spaces().space("main");
    var javadoc =
        ToolCall.of("javadoc")
            .add("-quiet")
            .add("--module", main.modules().names(","))
            .add("--module-source-path", main.modules().toModuleSourcePaths())
            .add("-d", workflow.folders().out("main", "api"))
            .add("-no" + "timestamp")
            .add("-encoding", "UTF-8")
            .add("-use")
            .add("-link" + "source")
            .add("-X" + "doc" + "lint:-missing")
            .add(
                "-group",
                "API",
                "se.jbee.inject:se.jbee.inject.api:se.jbee.inject.bind:se.jbee.inject.lang")
            .add("-group", "Container", "se.jbee.inject.bootstrap:se.jbee.inject.container")
            .add(
                "-group",
                "Add-ons",
                "se.jbee.inject.action:se.jbee.inject.event:se.jbee.inject.convert:se.jbee.inject.contract");

    run(javadoc);
  }
}
