package se.jbee.inject;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import se.jbee.inject.action.SuiteAction;
import se.jbee.inject.bind.SuiteBind;
import se.jbee.inject.container.SuiteContainer;
import se.jbee.inject.event.SuiteEvent;
import se.jbee.inject.scope.SuiteScope;

@RunWith(Suite.class)
@SuiteClasses({ SuitCore.class, SuiteContainer.class, SuiteBind.class,
		SuiteAction.class, SuiteScope.class, SuiteEvent.class })
public class SuiteAllFast {
	// all project tests
}
