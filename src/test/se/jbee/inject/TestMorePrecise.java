package se.jbee.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.util.Arrays;
import java.util.Comparator;

import org.junit.Test;

public class TestMorePrecise {

	static class HigherNumberIsMorePrecise
			implements MorePreciseThan<HigherNumberIsMorePrecise> {

		final int value;

		HigherNumberIsMorePrecise( int value ) {
			super();
			this.value = value;
		}

		@Override
		public boolean morePreciseThan( HigherNumberIsMorePrecise other ) {
			return value > other.value;
		}

	}

	static HigherNumberIsMorePrecise hip( int value ) {
		return new HigherNumberIsMorePrecise( value );
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
		HigherNumberIsMorePrecise[] values = new HigherNumberIsMorePrecise[] { hip( 1 ), hip( 2 ) };
		Arrays.sort( values, comparator(HigherNumberIsMorePrecise.class) );
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
	public void thatSpecificPackageIsMorePreciseThanSubPackagesUnderIt() {
		assertMorePrecise( Packages.packageOf( String.class ),
				Packages.subPackagesOf( String.class ) );
	}

	@Test
	public void thatPrecisionIsGivenByOrdinalStartingWithLowestPrecision() {
		DeclarationType[] types = DeclarationType.values();
		for ( int i = 1; i < types.length; i++ ) {
			assertTrue( types[i].morePreciseThan( types[i - 1] ) );
		}
	}

	@Test
	public void thatExplicitSourceIsMorePreciseThanAutoSource() {
		Source source = Source.source( TestMorePrecise.class );
		assertMorePrecise( source.typed( DeclarationType.EXPLICIT ),
				source.typed( DeclarationType.AUTO ) );
	}

	private static <T extends MorePreciseThan<? super T>> void assertMorePrecise( T morePrecise,
			T lessPrecise ) {
		assertTrue( morePrecise.morePreciseThan( lessPrecise ) );
		assertFalse( lessPrecise.morePreciseThan( morePrecise ) );
	}

	private static <T extends MorePreciseThan<? super T>> void assertNotMorePreciseThanItself( T type ) {
		assertFalse( type.morePreciseThan( type ) );
		assertEquals( 0, Instance.comparePrecision( type, type ) );
	}
	
	public static <T extends MorePreciseThan<? super T>> Comparator<T> comparator(@SuppressWarnings("unused") Class<T> cls) {
		return new PreciserThanComparator<>();
	}

	private static class PreciserThanComparator<T extends MorePreciseThan<? super T>>
			implements Comparator<T> {

		PreciserThanComparator() {
			// make visible
		}

		@Override
		public int compare( T one, T other ) {
			return Instance.comparePrecision( one, other );
		}

	}
	
}
