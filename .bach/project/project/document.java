package project;

import run.bach.ToolOperator;

public class document implements ToolOperator {
  @Override
  public void run(Operation operation) {
    operation.run("javadoc",
        """
        --module
          se.jbee.inject,se.jbee.inject.action,se.jbee.inject.api,se.jbee.inject.bind,se.jbee.inject.bootstrap,se.jbee.inject.container,se.jbee.inject.contract,se.jbee.inject.convert,se.jbee.inject.event,se.jbee.lang
        --module-source-path
          ./*/main/java
        -d
          .bach/out/main/api
        -notimestamp
        -encoding
          UTF-8
        -use
        -linksource
        -Xdoclint:-missing
        -group
          API
          se.jbee.inject:se.jbee.inject.api:se.jbee.inject.bind:se.jbee.inject.lang
        -group
          Container
          se.jbee.inject.bootstrap:se.jbee.inject.container
        -group
          Add-ons
          se.jbee.inject.action:se.jbee.inject.event:se.jbee.inject.convert:se.jbee.inject.contract
        """.lines().map(String::trim).toArray(String[]::new));
  }
}
