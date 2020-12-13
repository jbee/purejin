package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Name;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.jbee.inject.Name.named;

class TestName {

	@Test
	void exactSameNameShouldBeCompatible() {
		assertTrue(named("foo").isCompatibleWith(named("foo")));
	}

	@Test
	void wildcardShouldBeCompatibleToAnyName() {
		assertTrue(named("foo").isCompatibleWith(Name.ANY));
	}

	@Test
	void exactSameNameFollowedByWildcardShouldBeCompatible() {
		assertTrue(named("foo").isCompatibleWith(named("foo*")));
	}

	@Test
	void letterFollwoedByWildcardShouldBeCompatible() {
		assertTrue(named("foo").isCompatibleWith(named("f*")));
	}

	@Test
	void startOfNameFollowedByWildcardShouldBeCompatible() {
		assertTrue(named("foo").isCompatibleWith(named("fo*")));
	}

	@Test
	void defaultShouldBeCompatibleToAnyName() {
		assertTrue(Name.DEFAULT.isCompatibleWith(Name.ANY));
	}

	@Test
	void anyShouldBeCompatibleToDefaultName() {
		assertTrue(Name.ANY.isCompatibleWith(Name.DEFAULT));
	}

	@Test
	void anyShouldBeCompatibleToWhateverName() {
		assertTrue(Name.ANY.isCompatibleWith(named("foo")));
	}

	@Test
	void prefixShouldBeCompatibleToSamePrefix() {
		assertTrue(named("disk:").asPrefix().isCompatibleWith(
				named("disk:/home/jan/")));
	}

	@Test
	void prefixShouldBeCompatibleToSamePrefixInnerClassName() {
		String prefix = "se.jbee.inject.bind.testpropertyannotationbinds$property:*";
		String name = "se.jbee.inject.bind.testpropertyannotationbinds$property:foo";
		assertTrue(named(prefix).isCompatibleWith(named(name)));
	}

	@Test
	void infixShouldBeCompatibleToSameInfix() {
		Name foobar = named("foo*bar");
		assertTrue(foobar.isCompatibleWith(named("foobar")));
		assertTrue(foobar.isCompatibleWith(named("footerbar")));
	}

	@Test
	void infixShouldNotBeCompatibleWithPrefixPartOfInfix() {
		Name foobar = named("foo*bar");
		assertFalse(foobar.isCompatibleWith(named("foobbar")));
		assertFalse(foobar.isCompatibleWith(named("foo")));
	}
}
