package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.jbee.inject.DeclarationType.AUTO;
import static se.jbee.inject.DeclarationType.DEFAULT;
import static se.jbee.inject.DeclarationType.EXPLICIT;
import static se.jbee.inject.DeclarationType.IMPLICIT;
import static se.jbee.inject.DeclarationType.MULTI;
import static se.jbee.inject.DeclarationType.REQUIRED;

import org.junit.jupiter.api.Test;
import se.jbee.inject.DeclarationType;

public class TestDeclarationType {

	@Test
	public void thatExplicitIsNotReplacedByExplicit() {
		assertFalse(EXPLICIT.replacedBy(EXPLICIT));
	}

	@Test
	public void thatExplicitClashesWithExplicit() {
		assertTrue(EXPLICIT.clashesWith(EXPLICIT));
	}

	@Test
	public void thatExplicitClashesWithMultiple() {
		assertTrue(EXPLICIT.clashesWith(MULTI));
		assertTrue(MULTI.clashesWith(EXPLICIT));
	}

	@Test
	public void thatImplicitDefaultAutoReplacedByMulti() {
		assertTrue(IMPLICIT.replacedBy(MULTI));
		assertTrue(DEFAULT.replacedBy(MULTI));
		assertTrue(AUTO.replacedBy(MULTI));
	}

	@Test
	public void thatDefaultReplacedByAutoMultiOrExplicit() {
		assertTrue(DEFAULT.replacedBy(AUTO));
		assertTrue(DEFAULT.replacedBy(MULTI));
		assertTrue(DEFAULT.replacedBy(EXPLICIT));
	}

	@Test
	public void thatRequiredIsNotReplacedByAnyOtherType() {
		for (DeclarationType type : DeclarationType.values()) {
			if (type != REQUIRED) {
				assertFalse(REQUIRED.replacedBy(type), type.name());
			}
		}
	}

	@Test
	public void thatRequiredNotClashesWithRequired() {
		assertFalse(REQUIRED.clashesWith(REQUIRED));
	}

	@Test
	public void thatDefaultClashesWithDefault() {
		assertTrue(DEFAULT.clashesWith(DEFAULT));
	}

	@Test
	public void thatAutoNotClashesWithAuto() {
		assertFalse(AUTO.clashesWith(AUTO));
	}

	@Test
	public void thatImplicitReplacedByImplicit() {
		assertTrue(IMPLICIT.replacedBy(IMPLICIT));
	}

}
