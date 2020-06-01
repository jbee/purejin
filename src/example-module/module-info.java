module com.example.app {
  exports com.example.app;

  requires junit; // <- module we're testing with
  requires se.jbee.inject; // <- module under test

  opens com.example.test to junit, se.jbee.inject; // <- allow deep reflection

  provides se.jbee.inject.declare.Bundle with
      com.example.app.MyRootBundle,
      com.example.app.MyEnvSetupBundle;

  provides se.jbee.inject.declare.ModuleWith with
      com.example.app.SupportAnnotation;
}
