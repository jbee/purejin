package de.jbee.inject;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith ( Suite.class )
@SuiteClasses ( { TestAutobindBinds.class, TestElementBinds.class, TestInstanceBinds.class,
		TestRobotLegsBinds.class, TestServiceBinds.class, TestSupplierBinds.class,
		TestTypeBinds.class, TestName.class, TestType.class } )
public class TestSilk {
	// all project tests
}
