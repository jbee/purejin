import test.example1.MyEnvSetupBundle;
import test.example1.MyRootBundle;
import test.example1.SupportAnnotationPattern;

/**
 * Contains an example application used in tests to test the {@link
 * java.util.ServiceLoader} based features.
 */
@SuppressWarnings("Java9RedundantRequiresStatement") module test.examples {

  exports test.example1;

  requires transitive se.jbee.inject;

  provides se.jbee.inject.bind.Bundle with MyRootBundle, MyEnvSetupBundle;

  provides se.jbee.inject.bind.ModuleWith with SupportAnnotationPattern;
}
