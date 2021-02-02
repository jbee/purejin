package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Name;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.jbee.inject.Name.named;

class TestName {

	@Test
	void specificNameIsCompatibleWithItself() {
		assertTrue(named("foo").isCompatibleWith(named("foo")));
	}

	@Test
	void specificNameIsCompatibleWithAnyName() {
		assertTrue(named("foo").isCompatibleWith(Name.ANY));
	}

	@Test
	void specificNameIsCompatibleWithItselfAsPrefix() {
		assertTrue(named("foo").isCompatibleWith(named("foo*")));
	}

	@Test
	void specificNameIsCompatibleWithSingleLetterPrefix() {
		assertTrue(named("foo").isCompatibleWith(named("f*")));
	}

	@Test
	void specificNameIsCompatibleWithPartPrefix() {
		assertTrue(named("foo").isCompatibleWith(named("fo*")));
	}

	@Test
	void defaultIsCompatibleWithAnyName() {
		assertTrue(Name.DEFAULT.isCompatibleWith(Name.ANY));
	}

	@Test
	void anyIsCompatibleWithDefaultName() {
		assertTrue(Name.ANY.isCompatibleWith(Name.DEFAULT));
	}

	@Test
	void anyIsCompatibleWithSpecificName() {
		assertTrue(Name.ANY.isCompatibleWith(named("foo")));
	}

	@Test
	void patternIsNotCompatibleWithAnyName() {
		assertFalse(named("foo*").isCompatibleWith(Name.ANY));
	}

	@Test
	void prefixIsCompatibleWithNameHavingSamePrefix() {
		assertTrue(Name.ANY.in("disk").isCompatibleWith(
				named("disk:/home/jan/")));
	}

	@Test
	void prefixIsCompatibleWithSamePrefixInnerClassName() {
		String prefix = "se.jbee.inject.bind.testpropertyannotationbinds$property:*";
		String name = "se.jbee.inject.bind.testpropertyannotationbinds$property:foo";
		assertTrue(named(prefix).isCompatibleWith(named(name)));
	}

	@Test
	void infixIsCompatibleWithEmptyInsert() {
		assertTrue(named("foo*bar").isCompatibleWith(named("foobar")));
	}

	@Test
	void infixIsCompatibleWithRandomInsert() {
		assertTrue(named("foo*bar").isCompatibleWith(named("footerbar")));
	}

	@Test
	void infixIsNotCompatibleWithoutMatchingSuffix() {
		assertFalse(named("foo*bar").isCompatibleWith(named("footbaz")));
	}

	@Test
	void infixIsNotCompatibleWithPrefixPartOfInfix() {
		assertFalse(named("foo*bar").isCompatibleWith(named("foo")));
	}

	@Test
	void infixIsNotCompatibleWithPrefixWhenInsertStartsWithSuffix() {
		assertFalse(named("foo*bar").isCompatibleWith(named("foobbar")));
	}
}
