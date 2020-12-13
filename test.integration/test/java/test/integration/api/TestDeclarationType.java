package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.inject.DeclarationType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.jbee.inject.DeclarationType.*;

class TestDeclarationType {

	@Test
	void thatExplicitIsNotReplacedByExplicit() {
		assertFalse(EXPLICIT.replacedBy(EXPLICIT));
	}

	@Test
	void thatExplicitClashesWithExplicit() {
		assertTrue(EXPLICIT.clashesWith(EXPLICIT));
	}

	@Test
	void thatExplicitClashesWithMultiple() {
		assertTrue(EXPLICIT.clashesWith(MULTI));
		assertTrue(MULTI.clashesWith(EXPLICIT));
	}

	@Test
	void thatImplicitDefaultAutoReplacedByMulti() {
		assertTrue(IMPLICIT.replacedBy(MULTI));
		assertTrue(DEFAULT.replacedBy(MULTI));
		assertTrue(AUTO.replacedBy(MULTI));
	}

	@Test
	void thatDefaultReplacedByAutoMultiOrExplicit() {
		assertTrue(DEFAULT.replacedBy(AUTO));
		assertTrue(DEFAULT.replacedBy(MULTI));
		assertTrue(DEFAULT.replacedBy(EXPLICIT));
	}

	@Test
	void thatRequiredIsNotReplacedByAnyOtherType() {
		for (DeclarationType type : DeclarationType.values()) {
			if (type != REQUIRED) {
				assertFalse(REQUIRED.replacedBy(type), type.name());
			}
		}
	}

	@Test
	void thatRequiredNotClashesWithRequired() {
		assertFalse(REQUIRED.clashesWith(REQUIRED));
	}

	@Test
	void thatDefaultClashesWithDefault() {
		assertTrue(DEFAULT.clashesWith(DEFAULT));
	}

	@Test
	void thatAutoNotClashesWithAuto() {
		assertFalse(AUTO.clashesWith(AUTO));
	}

	@Test
	void thatImplicitReplacedByImplicit() {
		assertTrue(IMPLICIT.replacedBy(IMPLICIT));
	}

}
