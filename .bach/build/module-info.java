import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.ProjectInfo.Library;
import com.github.sormuras.bach.ProjectInfo.Main;
import com.github.sormuras.bach.ProjectInfo.Test;
import com.github.sormuras.bach.ProjectInfo.Tweak;

@ProjectInfo(
    name = "purejin",
    version = "19.1-ea",
    library = @Library(requires = {"org.junit.platform.console"}),
    main =
        @Main(
            release = 11,
            moduleSourcePaths = {"src/*/main/java", "src/*/main/java-9"},
            generateApiDocumentation = true,
            tweaks = {
              @Tweak(
                  tool = "javac",
                  args = {"-encoding", "UTF-8", "-g", "-parameters"}),
              @Tweak(
                  tool = "javadoc",
                  args = {
                    "-encoding",
                    "UTF-8",
                    "-use",
                    "-linksource",
                    "-notimestamp",
                    "-quiet",
                    "-Xdoclint:-missing",
                    //
                    "-group",
                    "API",
                    "se.jbee.inject:se.jbee.inject.api:se.jbee.inject.bind:se.jbee.inject.lang",
                    //
                    "-group",
                    "Container",
                    "se.jbee.inject.bootstrap:se.jbee.inject.container",
                    //
                    "-group",
                    "Add-ons",
                    "se.jbee.inject.action:se.jbee.inject.event:se.jbee.inject.convert"
                  })
            }),
    test = @Test(moduleSourcePaths = "src/*/test/java"))
module build {
  requires com.github.sormuras.bach;
}
