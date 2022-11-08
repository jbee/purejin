@run.bach.Project.Info(name = "purejin", version = "11-ea", targetsJava = 11)
module project {
  requires run.bach;

  provides run.bach.ToolOperator with
      project.document;
  provides run.bach.ToolTweak with
      project.PurejinConfigurator;
}
