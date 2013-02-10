package se.jbee.inject;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import se.jbee.inject.bind.SuiteBind;
import se.jbee.inject.service.SuiteService;
import se.jbee.inject.util.SuitUtil;

@RunWith ( Suite.class )
@SuiteClasses ( { TestName.class, TestType.class, TestPackages.class, TestMorePrecise.class,
		TestTarget.class, TestDeclarationType.class, SuitUtil.class, SuiteBind.class,
		SuiteService.class } )
public class SuiteSilk {
	// all project tests
}
