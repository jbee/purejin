/*
 *  Copyright (c) 2012-2020, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */

open /*test*/ module se.jbee.inject { // <- module under test
  exports se.jbee.inject;
  exports se.jbee.inject.action;
  exports se.jbee.inject.bind;
  // exports se.jbee.inject.bind.serviceloader; TODO No test is referencing a file from this package...
  exports se.jbee.inject.bootstrap;
  exports se.jbee.inject.config;
  exports se.jbee.inject.container;
  exports se.jbee.inject.declare;
  exports se.jbee.inject.event;
  exports se.jbee.inject.extend;
  exports se.jbee.inject.scope;

  requires transitive java.logging;
  requires java.desktop;
  requires java.management;
  requires junit; // <- module we're testing with

  uses se.jbee.inject.declare.Bundle;
  uses se.jbee.inject.declare.ModuleWith;
}
