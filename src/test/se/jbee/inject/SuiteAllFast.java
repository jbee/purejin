package se.jbee.inject;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import se.jbee.inject.action.SuiteAction;
import se.jbee.inject.bind.SuiteBind;
import se.jbee.inject.container.SuitContainer;
import se.jbee.inject.event.SuiteEvent;
import se.jbee.inject.scope.SuitScope;

@RunWith(Suite.class)
@SuiteClasses({ SuitCore.class, SuitContainer.class, SuiteBind.class,
		SuiteAction.class, SuitScope.class, SuiteEvent.class })
public class SuiteAllFast {
	// all project tests
}
