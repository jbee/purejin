import static com.github.sormuras.bach.api.ExternalLibraryName.JUNIT;

import com.github.sormuras.bach.api.ProjectInfo;
import com.github.sormuras.bach.api.ProjectInfo.*;

@ProjectInfo(
        name = "purejin",
        version = "8.2-ea",
        main = @Main(javaRelease = 8, jarWithSources = true),
        external = @External(libraries = @ExternalLibrary(name = JUNIT, version = "5.8.0-M1")),
        tool = @Tool(
                skip = {"jdeps", "jlink"},
                tweaks = {
                    @Tweak(
                        tool = "javac",
                        with = """
                            -g
                            -encoding
                              UTF-8
                            -parameters
                            -Xlint
                            """),
                    @Tweak(
                        tool = "javadoc",
                        with = """
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
                            """),
                    @Tweak(tool = "junit(test.integration)", with = "--fail-if-no-tests")
                })
        )
module bach.info {
    requires com.github.sormuras.bach;
}
