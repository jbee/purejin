package se.jbee.inject.container;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestTypecast.class, TestScopes.class })
public class SuitContainer {
	// tests of the container package
}
