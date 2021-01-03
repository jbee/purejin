package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.inject.DeclarationType;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.DeclarationType.*;

class TestDeclarationType {

	@Test
	void explicitIsNotReplacedByExplicit() {
		assertFalse(EXPLICIT.replacedBy(EXPLICIT));
	}

	@Test
	void explicitClashesWithExplicit() {
		assertTrue(EXPLICIT.clashesWith(EXPLICIT));
	}

	@Test
	void explicitClashesWithMultiple() {
		assertTrue(EXPLICIT.clashesWith(MULTI));
		assertTrue(MULTI.clashesWith(EXPLICIT));
	}

	@Test
	void implicitDefaultContractAreReplacedByMulti() {
		assertTrue(IMPLICIT.replacedBy(MULTI));
		assertTrue(DEFAULT.replacedBy(MULTI));
		assertTrue(CONTRACT.replacedBy(MULTI));
	}

	@Test
	void defaultIsReplacedByContractMultiOrExplicit() {
		assertTrue(DEFAULT.replacedBy(CONTRACT));
		assertTrue(DEFAULT.replacedBy(MULTI));
		assertTrue(DEFAULT.replacedBy(EXPLICIT));
	}

	@Test
	void requiredIsNotReplacedByAnyOtherType() {
		for (DeclarationType type : DeclarationType.values()) {
			if (type != REQUIRED) {
				assertFalse(REQUIRED.replacedBy(type), type.name());
			}
		}
	}

	@Test
	void requiredDoesNotClashWithRequired() {
		assertFalse(REQUIRED.clashesWith(REQUIRED));
	}

	@Test
	void defaultClashesWithDefault() {
		assertTrue(DEFAULT.clashesWith(DEFAULT));
	}

	@Test
	void contractDoesNotClashWithContract() {
		assertFalse(CONTRACT.clashesWith(CONTRACT));
	}

	@Test
	void implicitIsReplacedByImplicit() {
		assertTrue(IMPLICIT.replacedBy(IMPLICIT));
	}

	/**
	 * The letters are used as ID when printing the type so they should be unique
	 */
	@Test
	void firstLetterIsUnique() {
		assertEquals(values().length,
				stream(values()).map(e -> e.name().charAt(0)) //
						.collect(toSet()).size());
	}
}
