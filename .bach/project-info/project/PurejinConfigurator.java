package project;

import com.github.sormuras.bach.Configurator;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.ToolCallTweak;

public class PurejinConfigurator implements Configurator {

	@Override
	public Project configureProject(Project project) {
		return project
				.withName("purejin")
				.withVersion("8.2-ea")
				.withTargetsJava(11)
				.withTweak("main", ToolCallTweak.WORKFLOW_COMPILE_CLASSES_JAVAC,
						javac -> javac.with("-g").with("-encoding",
						"UTF-8").with(
						"-parameters").with(
						"-Xlint:-missing-explicit-ctor"))
				.withTweak("test", ToolCallTweak.WORKFLOW_COMPILE_CLASSES_JAVAC,
						javac -> javac.with("-g").with("-encoding",
								"UTF-8").with(
								"-parameters").with(
								"-Xlint:-serial"))
				;
	}
}
