package test.integration.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.jbee.inject.Name.named;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Name;

public class TestName {

	@Test
	public void exactSameNameShouldBeCompatible() {
		assertTrue(named("foo").isCompatibleWith(named("foo")));
	}

	@Test
	public void wildcardShouldBeCompatibleToAnyName() {
		assertTrue(named("foo").isCompatibleWith(Name.ANY));
	}

	@Test
	public void exactSameNameFollowedByWildcardShouldBeCompatible() {
		assertTrue(named("foo").isCompatibleWith(named("foo*")));
	}

	@Test
	public void letterFollwoedByWildcardShouldBeCompatible() {
		assertTrue(named("foo").isCompatibleWith(named("f*")));
	}

	@Test
	public void startOfNameFollowedByWildcardShouldBeCompatible() {
		assertTrue(named("foo").isCompatibleWith(named("fo*")));
	}

	@Test
	public void defaultShouldBeCompatibleToAnyName() {
		assertTrue(Name.DEFAULT.isCompatibleWith(Name.ANY));
	}

	@Test
	public void anyShouldBeCompatibleToDefaultName() {
		assertTrue(Name.ANY.isCompatibleWith(Name.DEFAULT));
	}

	@Test
	public void anyShouldBeCompatibleToWhateverName() {
		assertTrue(Name.ANY.isCompatibleWith(named("foo")));
	}

	@Test
	public void prefixShouldBeCompatibleToSamePrefix() {
		assertTrue(named("disk:").asPrefix().isCompatibleWith(
				named("disk:/home/jan/")));
	}

	@Test
	public void prefixShouldBeCompatibleToSamePrefixInnerClassName() {
		String prefix = "se.jbee.inject.bind.testpropertyannotationbinds$property:*";
		String name = "se.jbee.inject.bind.testpropertyannotationbinds$property:foo";
		assertTrue(named(prefix).isCompatibleWith(named(name)));
	}

	@Test
	public void infixShouldBeCompatibleToSameInfix() {
		Name foobar = named("foo*bar");
		assertTrue(foobar.isCompatibleWith(named("foobar")));
		assertTrue(foobar.isCompatibleWith(named("footerbar")));
	}

	@Test
	public void infixShouldNotBeCompatibleWithPrefixPartOfInfix() {
		Name foobar = named("foo*bar");
		assertFalse(foobar.isCompatibleWith(named("foobbar")));
		assertFalse(foobar.isCompatibleWith(named("foo")));
	}
}
