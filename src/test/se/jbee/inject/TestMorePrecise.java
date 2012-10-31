package se.jbee.inject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.util.Arrays;

import org.junit.Test;

public class TestMorePrecise {

	static class HigherIsPreciser
			implements PreciserThan<HigherIsPreciser> {

		final int value;

		HigherIsPreciser( int value ) {
			super();
			this.value = value;
		}

		@Override
		public boolean morePreciseThan( HigherIsPreciser other ) {
			return value > other.value;
		}

	}

	static HigherIsPreciser hip( int value ) {
		return new HigherIsPreciser( value );
	}

	@Test
	public void thatMorePreciseEvalsToTrue() {
		assertTrue( hip( 2 ).morePreciseThan( hip( 1 ) ) );
	}

	@Test
	public void thatEqualPreciseEvalsToFalse() {
		assertFalse( hip( 2 ).morePreciseThan( hip( 2 ) ) );
	}

	@Test
	public void thatLessPreciseEvalsToFalse() {
		assertFalse( hip( 1 ).morePreciseThan( hip( 2 ) ) );
	}

	@Test
	public void thatMorePreciseComesFirstInSortOrder() {
		HigherIsPreciser[] values = new HigherIsPreciser[] { hip( 1 ), hip( 2 ) };
		Arrays.sort( values, Precision.<HigherIsPreciser> comparator() );
		assertTrue( values[0].value == 2 );
	}

	@Test
	public void thatSameTypeIsNotMorePrecise() {
		assertNotMorePreciseThanItself( Type.raw( String.class ) );
	}

	@Test
	public void thatSameDefaultNameIsNotMorePrecise() {
		assertNotMorePreciseThanItself( Name.DEFAULT );
	}

	@Test
	public void thatUnnamedIsMorePreciseThanNamedInstance() {
		Type<Integer> type = raw( Integer.class );
		Instance<Integer> named = instance( named( "foo" ), type );
		Instance<Integer> unnamed = defaultInstanceOf( type );
		assertMorePrecise( unnamed, named );
	}

	@Test
	public void thatUnnamedIsMorePreciseThanNamed() {
		assertTrue( Name.DEFAULT.morePreciseThan( named( "foo" ) ) );
	}

	@Test
	public void thatNamedIsNotMorePreciseThanUnnamed() {
		assertFalse( named( "bar" ).morePreciseThan( Name.DEFAULT ) );
	}

	@Test
	public void thatSameSpecificPackageIsNotMorePrecise() {
		assertNotMorePreciseThanItself( Packages.packageOf( String.class ) );
	}

	@Test
	public void thatSpecificPackageIsMorePreciseThanGlobal() {
		assertMorePrecise( Packages.packageOf( String.class ), Packages.ALL );
	}

	@Test
	public void thatSpecificPackageIsMorePreciseThanThatPackageWithItsSubPackages() {
		assertMorePrecise( Packages.packageOf( String.class ),
				Packages.packageAndSubPackagesOf( String.class ) );
	}

	@Test
	public void thatSpecificPackageIsNotMorePreciseThanSubPackagesUnderIt() {
		assertEqualPrecise( Packages.packageOf( String.class ),
				Packages.subPackagesOf( String.class ) );
	}

	private <T extends PreciserThan<? super T>> void assertEqualPrecise( T one, T other ) {
		assertFalse( one.morePreciseThan( other ) );
		assertFalse( other.morePreciseThan( one ) );
	}

	private <T extends PreciserThan<? super T>> void assertMorePrecise( T morePrecise, T lessPrecise ) {
		assertTrue( morePrecise.morePreciseThan( lessPrecise ) );
		assertFalse( lessPrecise.morePreciseThan( morePrecise ) );
	}

	private <T extends PreciserThan<? super T>> void assertNotMorePreciseThanItself( T type ) {
		assertFalse( type.morePreciseThan( type ) );
		assertThat( Precision.comparePrecision( type, type ), is( 0 ) );
	}
}
