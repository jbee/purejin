open /*test*/ module se.jbee.inject /*overrides _main_ module*/ {
  exports se.jbee.inject;
  exports se.jbee.inject.action;
  exports se.jbee.inject.bind;
  exports se.jbee.inject.bind.serviceloader;
  exports se.jbee.inject.bootstrap;
  exports se.jbee.inject.config;
  exports se.jbee.inject.container;
  exports se.jbee.inject.declare;
  exports se.jbee.inject.event;
  exports se.jbee.inject.extend;
  exports se.jbee.inject.scope;

  requires java.logging;
  requires java.desktop;
  requires java.management;
  requires junit;

  uses se.jbee.inject.declare.Bundle;
  uses se.jbee.inject.declare.ModuleWith;
}
