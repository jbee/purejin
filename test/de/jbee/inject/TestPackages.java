package de.jbee.inject;

import static de.jbee.inject.Packages.packages;

import org.junit.Test;

public class TestPackages {

	@Test
	public void thatPackageNamesAreValid() {
		packages( "java.lang" );
	}

	@Test ( expected = IllegalArgumentException.class )
	public void thatPackgeNamesPlusDotAreNotValid() {
		packages( "java.lang." );
	}

	@Test
	public void thatPackageNamesPlusDotAndStarAreValid() {
		packages( "java.lang.*" );
	}

	@Test
	public void thatPackageNamesPlusStarAreValid() {
		packages( "java.lang*" );
	}

	@Test ( expected = IllegalArgumentException.class )
	public void thatPackageNamesWithMinusAreNotValid() {
		packages( "java-lang.something" );
	}

	@Test
	public void thatPackageNameJustStarIsValid() {
		packages( "*" );
	}

	@Test ( expected = IllegalArgumentException.class )
	public void thatPackageNameJustDotAndStarIsNotValid() {
		packages( ".*" );
	}
}
