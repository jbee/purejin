import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.Externals;
import com.github.sormuras.bach.ProjectInfo.Tools;
import com.github.sormuras.bach.ProjectInfo.Tweak;

@ProjectInfo(
        name = "purejin",
        version = "8.2-ea",
        compileModulesForJavaRelease = 8,
        includeSourceFilesIntoModules = true,
        lookupExternals = @Externals(name = Externals.Name.JUNIT, version = "5.8.0-M1"),
        tools = @Tools(skip = {"jdeps", "jlink"}),
        tweaks = {
                @Tweak(tool = "javac", option = "-g", value = {"-encoding", "UTF-8", "-parameters"}),
                @Tweak(tool = "javadoc", option = "-notimestamp",
                        value = {
                                "-encoding", "UTF-8",
                                "-use",
                                "-linksource",
                                "-Xdoclint:-missing",
                                //
                                "-group", "API",
                                "se.jbee.inject:se.jbee.inject.api:se.jbee.inject.bind:se.jbee.inject.lang",
                                //
                                "-group", "Container",
                                "se.jbee.inject.bootstrap:se.jbee.inject.container",
                                //
                                "-group", "Add-ons",
                                "se.jbee.inject.action:se.jbee.inject.event:se.jbee.inject.convert:se.jbee.inject.contract"
                        }),
                @Tweak(tool = "junit",
                        option = "--fail-if-no-tests",
                        value = {
                                "--config=junit.jupiter.execution.parallel.enabled=true",
                                "--config=junit.jupiter.execution.parallel.mode.default=concurrent"
                        })
        })
module bach.info {
    requires com.github.sormuras.bach;
}
