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
	void implicitDefaultPublishedAreReplacedByMulti() {
		assertTrue(IMPLICIT.replacedBy(MULTI));
		assertTrue(DEFAULT.replacedBy(MULTI));
		assertTrue(PUBLISHED.replacedBy(MULTI));
	}

	@Test
	void defaultIsReplacedByPublishedMultiOrExplicit() {
		assertTrue(DEFAULT.replacedBy(PUBLISHED));
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
	void publishedDoesNotClashWithPublished() {
		assertFalse(PUBLISHED.clashesWith(PUBLISHED));
	}

	@Test
	void implicitIsReplacedByImplicit() {
		assertTrue(IMPLICIT.replacedBy(IMPLICIT));
	}

	/**
	 * The letters are used as ID when printing the type so they should be
	 * unique, except {@link DeclarationType#PUBLISHED} and {@link
	 * DeclarationType#PROVIDED}.
	 */
	@Test
	void firstLetterIsUnique() {
		assertEquals(values().length -1,
				stream(values()).map(e -> e.name().charAt(0)) //
						.collect(toSet()).size());
	}
}
