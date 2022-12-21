package project;

import run.bach.Composer;
import run.bach.Tweaks;

public class PurejinConfigurator extends Composer {
  @Override
  public Tweaks createTweaks() {
    return new Tweaks(
        call ->
            switch (call.tool()) {
              case "javac" -> call.with("-g")
                  .with("-encoding", "UTF-8")
                  .with("-parameters")
                  .with("-Xlint:-missing-explicit-ctor,-serial");
              case "junit" -> call.with("--details", "NONE").with("--disable-banner");
              default -> call;
            });
  }
}
