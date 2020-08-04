module com.example.test {

  requires junit; // <- module we're testing with
  requires com.example.app; // <- module under test

  opens com.example.test to junit, se.jbee.inject.api; // <- allow deep reflection

}
