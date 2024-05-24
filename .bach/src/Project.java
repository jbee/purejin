import java.io.File;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import run.bach.ModuleFinders;
import run.bach.ToolCall;
import run.bach.ToolRunner;
import run.bach.workflow.Builder;
import run.bach.workflow.Folders;
import run.bach.workflow.Structure;
import run.bach.workflow.Structure.Basics;
import run.bach.workflow.Structure.DeclaredModule;
import run.bach.workflow.Structure.Space;
import run.bach.workflow.Structure.Spaces;
import run.bach.workflow.Workflow;
import run.info.org.junit.JUnit;

record Project(Workflow workflow) implements Builder {
  static final String VERSION = System.getProperty("--project-version", "11-ea");

  static Project ofCurrentWorkingDirectory() {
    var basics = new Basics("purejin", VERSION);
    var main =
        new Space("main")
            .withTargetingJavaRelease(11)
            .with(mainModule("se.jbee.inject"))
            .with(mainModule("se.jbee.inject.action"))
            .with(mainModule("se.jbee.inject.api"))
            .with(mainModule("se.jbee.inject.bind"))
            .with(mainModule("se.jbee.inject.bootstrap"))
            .with(mainModule("se.jbee.inject.container"))
            .with(mainModule("se.jbee.inject.contract"))
            .with(mainModule("se.jbee.inject.convert"))
            .with(mainModule("se.jbee.inject.event"))
            .with(mainModule("se.jbee.lang"));
    var test =
        new Space("test", main)
            .with(testModule("se.jbee.junit.assertion"))
            .with(testModule("test.examples"))
            .with(testModule("test.integration"));

    var libraries = ModuleFinder.compose(ModuleFinders.ofProperties(JUnit.MODULES));
    return new Project(
        new Workflow(
            Folders.ofCurrentWorkingDirectory(),
            new Structure(basics, new Spaces(main, test), libraries),
            ToolRunner.ofSystem()));
  }

  private static DeclaredModule mainModule(String module) {
    return declareModule("main", module);
  }

  private static DeclaredModule testModule(String module) {
    return declareModule("test", module);
  }

  private static DeclaredModule declareModule(String space, String module) {
    var content = Path.of(module);
    return new DeclaredModule(content, content.resolve(space + "/java/module-info.java"));
  }

  @Override
  public ToolCall classesCompilerUsesJavacToolCall() {
    return ToolCall.of("javac")
        .add("-g")
        .add("-encoding", "UTF-8")
        .add("-parameters")
        .add("-X" + "lint:-missing-explicit-ctor,-serial");
  }

  @Override
  public void junitTesterRunJUnitToolCall(ToolCall junit) {
    run(junit.add("--details", "NONE").add("--disable-banner"));
  }

  void document() {
    var main = workflow.structure().spaces().space("main");
    var moduleSourcePaths = main.modules().toModuleSourcePaths();
    var moduleSourcePath = String.join(File.pathSeparator, moduleSourcePaths);
    var javadoc =
        ToolCall.of("javadoc")
            .add("-quiet")
            .add("--module", main.modules().names(","))
            .add("--module-source-path", moduleSourcePath)
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
