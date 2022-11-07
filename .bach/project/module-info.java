module project {
    requires run.bach;
    provides run.bach.Project.Composer with project.PurejinConfigurator;
    provides run.bach.ToolOperator with project.document;
    provides run.bach.ToolTweak with project.PurejinConfigurator;
}
