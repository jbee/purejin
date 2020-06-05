module com.example.app {

  exports com.example.app;
  requires transitive se.jbee.inject;

  provides se.jbee.inject.declare.Bundle with
      com.example.app.MyRootBundle,
      com.example.app.MyEnvSetupBundle;

  provides se.jbee.inject.declare.ModuleWith with
      com.example.app.SupportAnnotation;
}
