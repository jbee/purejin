import com.github.sormuras.bach.project.Feature;
import com.github.sormuras.bach.project.ProjectInfo;
import com.github.sormuras.bach.project.ProjectInfo.Tweak;

@ProjectInfo(
        name = "purejin",
        version = "8.2-ea",
        compileModulesForJavaRelease = 8,
        features = {
                Feature.GENERATE_API_DOCUMENTATION,
                Feature.INCLUDE_SOURCES_IN_MODULAR_JAR
        },
        tweaks = {
                @Tweak(tool = "javac", with = {"-encoding", "UTF-8", "-g", "-parameters"}),
                @Tweak(tool = "javadoc",
                        with = {
                                "-encoding", "UTF-8",
                                "-use",
                                "-linksource",
                                "-notimestamp",
                                "-quiet",
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
                        with = {
                                "--config=junit.jupiter.execution.parallel.enabled=true",
                                "--config=junit.jupiter.execution.parallel.mode.default=concurrent"
                        })
        })
module build {
    requires com.github.sormuras.bach;
}
