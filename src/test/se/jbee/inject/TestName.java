package se.jbee.inject;

import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Name.named;

import org.junit.Test;

public class TestName {

	@Test
	public void exactSameNameShouldBeApplicable() {
		assertTrue( named( "foo" ).isApplicableFor( named( "foo" ) ) );
	}

	@Test
	public void wildcardShouldBeApplicableToAnyName() {
		assertTrue( named( "foo" ).isApplicableFor( Name.ANY ) );
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
	public void anyShouldBeApplicableToDefaultName() {
		assertTrue( Name.ANY.isApplicableFor( Name.DEFAULT ) );
	}

	@Test
	public void anyShouldBeApplicableToWhateverName() {
		assertTrue( Name.ANY.isApplicableFor( named( "foo" ) ) );
	}
}
