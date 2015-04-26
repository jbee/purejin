package se.jbee.inject.procedure;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith ( Suite.class )
@SuiteClasses ( { TestProcedureBinds.class, TestServiceBinds.class, TestCommandBinds.class,
		TestProcedureInspectorBinds.class, TestServiceInvocationBinds.class } )
public class SuiteProcedure {
	// all tests in the service package
}
