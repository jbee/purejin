import test.example.app.MyEnvSetupBundle;
import test.example.app.MyRootBundle;
import test.example.app.SupportAnnotationPattern;

/**
 * Contains an example application used in tests to test the {@link
 * java.util.ServiceLoader} based features.
 */
@SuppressWarnings("Java9RedundantRequiresStatement") module test.example.app {

  exports test.example.app;

  requires transitive se.jbee.inject;

  provides se.jbee.inject.bind.Bundle with MyRootBundle, MyEnvSetupBundle;

  provides se.jbee.inject.bind.ModuleWith with SupportAnnotationPattern;
}
