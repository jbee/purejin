/**
 * Contains an example application used in tests to test the {@link
 * java.util.ServiceLoader} based features.
 */
@SuppressWarnings("Java9RedundantRequiresStatement") module com.example.app {

  exports com.example.app;

  requires transitive se.jbee.inject;

  provides se.jbee.inject.bind.Bundle with
      com.example.app.MyRootBundle,
      com.example.app.MyEnvSetupBundle;

  provides se.jbee.inject.bind.ModuleWith with
      com.example.app.SupportAnnotation;
}
