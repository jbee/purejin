package se.jbee.inject;

import static org.junit.Assert.assertTrue;
import static se.jbee.inject.DeclarationType.AUTO;
import static se.jbee.inject.DeclarationType.DEFAULT;
import static se.jbee.inject.DeclarationType.EXPLICIT;
import static se.jbee.inject.DeclarationType.IMPLICIT;
import static se.jbee.inject.DeclarationType.MULTI;

import org.junit.Test;

public class TestDeclarationType {

	@Test
	public void thatExplicitIsReplacedByExplicit() {
		assertTrue( EXPLICIT.replacedBy( EXPLICIT ) );
	}

	@Test
	public void thatExplicitClashesWithExplicit() {
		assertTrue( EXPLICIT.clashesWith( EXPLICIT ) );
	}

	@Test
	public void thatExplicitClashesWithMultiple() {
		assertTrue( EXPLICIT.clashesWith( MULTI ) );
		assertTrue( MULTI.clashesWith( EXPLICIT ) );
	}

	@Test
	public void thatImplicitDefaultAutoReplacedByMulti() {
		assertTrue( IMPLICIT.replacedBy( MULTI ) );
		assertTrue( DEFAULT.replacedBy( MULTI ) );
		assertTrue( AUTO.replacedBy( MULTI ) );
	}

	@Test
	public void thatDefaultReplacedByAutoMultiOrExplicit() {
		assertTrue( DEFAULT.replacedBy( AUTO ) );
		assertTrue( DEFAULT.replacedBy( MULTI ) );
		assertTrue( DEFAULT.replacedBy( EXPLICIT ) );
	}

}
