package de.jbee.inject;

import static de.jbee.inject.DeclarationType.EXPLICIT;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestDeclarationType {

	@Test
	public void thatExplicitIsReplacedByExplicit() {
		assertTrue( EXPLICIT.replacedBy( EXPLICIT ) );
	}
}
