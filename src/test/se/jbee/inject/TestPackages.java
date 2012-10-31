package se.jbee.inject;

import static org.junit.Assert.assertFalse;
import static se.jbee.inject.Packages.packageOf;
import static se.jbee.inject.Packages.packages;

import java.util.List;

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

	@Test
	public void thatLowerBoundTypeIsNotInPackageJavaLang() {
		Packages javaLang = packageOf( String.class );
		assertFalse( javaLang.contains( Type.WILDCARD ) );
		assertFalse( javaLang.contains( Type.raw( List.class ).asLowerBound() ) );
	}
}
