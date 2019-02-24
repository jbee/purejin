package se.jbee.inject;

import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Name.named;

import org.junit.Test;

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
}
