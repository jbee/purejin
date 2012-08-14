package de.jbee.inject.bind;

import de.jbee.inject.bind.BasicConfigurator;
import de.jbee.inject.util.Scoped;

public class TestContext {

	void test( BasicConfigurator context ) {
		// we simply cannot know what is a realistic assumption but we want to validate it so we can warn about injections that will not work as expected. 
		context.let( Scoped.THREAD ).outlive( Scoped.APPLICATION );
	}
}
