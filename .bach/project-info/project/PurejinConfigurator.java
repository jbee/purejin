package project;

import com.github.sormuras.bach.Configurator;
import com.github.sormuras.bach.Project;

public class PurejinConfigurator implements Configurator {
	@Override
	public Project configureProject(Project project) {
		return project
				.withName("purejin")
				.withVersion("8.2-ea")
				.withTargetsJava(11)
				.withAdditionalCompileJavacArguments("main", "-g", "-encoding",
						"UTF-8",
						"-parameters",
						"-Xlint")
				.withAdditionalCompileJavacArguments("test", "-g", "-encoding",
						"UTF-8",
						"-parameters",
						"-Xlint")
				.withExternalModules("junit", "5.8.2");
	}
}
