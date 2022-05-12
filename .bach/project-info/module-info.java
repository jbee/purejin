import com.github.sormuras.bach.Configurator;

/*
@ProjectInfo(
        tool = @Tool(
                skip = {"jdeps", "jlink"},
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
*/
module project {
    requires com.github.sormuras.bach;
    provides Configurator with project.PurejinConfigurator;
}
