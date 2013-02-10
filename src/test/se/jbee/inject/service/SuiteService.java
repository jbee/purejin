package se.jbee.inject.service;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith ( Suite.class )
@SuiteClasses ( { TestServiceMethodBinds.class, TestServiceBinds.class, TestCommandBinds.class,
		TestServiceInspectorBinds.class, TestExtensionBinds.class,
		TestServiceInvocationBinds.class, } )
public class SuiteService {
	// all tests in the service package
}
