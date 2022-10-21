package run.bach.project;

import run.bach.ToolCall;
import run.bach.ToolOperator;

public class LaunchTool implements ToolOperator {
  public LaunchTool() {}

  @Override
  public String name() {
    return "launch";
  }

  @Override
  public void run(Operation operation) {
    var bach = operation.bach();
    var main = bach.project().spaces().main();
    var launcher = main.launcher().orElseThrow(() -> new Error("No launcher defined. No start."));
    var java =
        ToolCall.of("java")
            .with("--module-path", bach.paths().out("main", "modules"))
            .with("--module", launcher)
            .with(operation.arguments().stream());
    bach.run(java);
  }
}
