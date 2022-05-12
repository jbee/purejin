import com.github.sormuras.bach.Configurator;
import java.util.spi.ToolProvider;

module project {
    requires com.github.sormuras.bach;
    provides Configurator with project.PurejinConfigurator;
    provides ToolProvider with project.Document;
}
