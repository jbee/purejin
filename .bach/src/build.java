import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.external.JUnit;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ProjectSpace;

class build {
  public static void main(String... args) {
    Bach.build(build::project);
    /*
    -notimestamp
    -encoding
      UTF-8
    -use
    -linksource
    -Xdoclint:-missing
    -group
      API
      se.jbee.inject:se.jbee.inject.api:se.jbee.inject.bind:se.jbee.inject.lang
    -group
      Container
      se.jbee.inject.bootstrap:se.jbee.inject.container
    -group
      Add-ons
      se.jbee.inject.action:se.jbee.inject.event:se.jbee.inject.convert:se.jbee.inject.contract
     */
  }

  static Project project(Project project) {
    return project
        .withName("purejin")
        .withVersion("8.2-ea")
        .withMainSpace(build::main)
        .withTestSpace(build::test)
        .withRequiresExternalModules("org.junit.platform.console")
        .withExternalModuleLocators(JUnit.V_5_8_0_M1);
  }

  static ProjectSpace main(ProjectSpace main) {
    return main.withJavaRelease(8)
        .withModuleSourcePaths("./*/main/java", "./*/main/java-module")
        .withModule("se.jbee.inject/main/java-module/module-info.java")
        .withModule("se.jbee.inject.action/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.api/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.bind/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.bootstrap/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.container/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.contract/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.convert/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.inject.event/main/java-module/module-info.java", build::main)
        .withModule("se.jbee.lang/main/java-module/module-info.java", build::main);
  }

  static DeclaredModule main(DeclaredModule module) {
    return module.withSources(module.name() + "/main/java");
  }

  static ProjectSpace test(ProjectSpace test) {
    return test.withModuleSourcePaths("./*/test/java")
        .withModule("se.jbee.junit.assertion/test/java/module-info.java")
        .withModule(
            "test.examples/test/java/module-info.java",
            module -> module.withResources("test.examples/test/resources"))
        .withModule(
            "test.integration/test/java/module-info.java",
            module -> module.withResources("test.integration/test/resources"))
        .withModulePaths(".bach/workspace/modules", ".bach/external-modules");
  }
}
