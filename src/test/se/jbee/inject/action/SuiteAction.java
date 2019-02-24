package se.jbee.inject.action;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestActionBinds.class, TestServiceBinds.class,
		TestCommandBinds.class, TestActionInspectorBinds.class,
		TestServiceInvocationBinds.class })
public class SuiteAction {
	// all tests in the action package
}
