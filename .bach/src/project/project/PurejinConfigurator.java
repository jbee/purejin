package project;

import run.bach.Project;
import run.bach.ToolCall;
import run.bach.ToolTweak;

public class PurejinConfigurator implements Project.Composer, ToolTweak {

  @Override
  public Project composeProject(Project project) {
    return project
        .withName("purejin")
        .withVersion("11-ea")
        .withTargetsJava(11)
        .withRequiresModule("org.junit.jupiter")
        .withRequiresModule("org.junit.platform.console")
        .withRequiresModule("org.junit.platform.jfr");
  }

  @Override
  public ToolCall tweak(ToolCall call) {
    return switch (call.name()) {
      case "javac" -> call.with("-g")
          .with("-encoding", "UTF-8")
          .with("-parameters")
          .with("-Xlint:-missing-explicit-ctor,-serial");
      case "junit" -> call.with("--details", "NONE").with("--disable-banner");
      default -> call;
    };
  }
}
