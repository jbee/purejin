package de.jbee.inject;

import static de.jbee.inject.Name.named;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestName {

	@Test
	public void exactSameNameShouldBeApplicable() {
		assertTrue( named( "foo" ).isApplicableFor( named( "foo" ) ) );
	}

	@Test
	public void wildcardShouldBeApplicableToAnyName() {
		assertTrue( named( "foo" ).isApplicableFor( named( "*" ) ) );
	}

	@Test
	public void exactSameNameFollowedByWildcardShouldBeApplicable() {
		assertTrue( named( "foo" ).isApplicableFor( named( "foo*" ) ) );
	}

	@Test
	public void letterFollwoedByWildcardShouldBeApplicable() {
		assertTrue( named( "foo" ).isApplicableFor( named( "f*" ) ) );
	}

	@Test
	public void startOfNameFollowedByWildcardShouldBeApplicable() {
		assertTrue( named( "foo" ).isApplicableFor( named( "fo*" ) ) );
	}

	@Test
	public void defaultShouldBeApplicableToAnyName() {
		assertTrue( Name.DEFAULT.isApplicableFor( Name.ANY ) );
	}

	@Test
	public void anyShouldBeApplicableToWhateverName() {
		assertTrue( Name.ANY.isApplicableFor( named( "foo" ) ) );
	}
}
