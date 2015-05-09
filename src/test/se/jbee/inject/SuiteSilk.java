package se.jbee.inject;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import se.jbee.inject.action.SuiteAction;
import se.jbee.inject.bind.SuiteBind;
import se.jbee.inject.container.SuitContainer;

@RunWith ( Suite.class )
@SuiteClasses ( { TestName.class, TestType.class, TestPackages.class, TestMorePrecise.class,
		TestTarget.class, TestDeclarationType.class,
		// suits
		SuitContainer.class, SuiteBind.class, SuiteAction.class } )
public class SuiteSilk {
	// all project tests
}
