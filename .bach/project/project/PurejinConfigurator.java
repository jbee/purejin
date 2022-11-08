package project;

import run.bach.ToolCall;
import run.bach.ToolTweak;

public class PurejinConfigurator implements ToolTweak {
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
