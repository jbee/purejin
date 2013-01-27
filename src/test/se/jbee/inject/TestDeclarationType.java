package se.jbee.inject;

import static org.junit.Assert.assertTrue;
import static se.jbee.inject.DeclarationType.EXPLICIT;

import org.junit.Test;

public class TestDeclarationType {

	@Test
	public void thatExplicitIsReplacedByExplicit() {
		assertTrue( EXPLICIT.replacedBy( EXPLICIT ) );
	}

}
