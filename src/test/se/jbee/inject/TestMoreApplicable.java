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

	static class HigherNumberIsMoreApplicable
			implements MoreApplicableThan<HigherNumberIsMoreApplicable> {

		final int value;

		HigherNumberIsMoreApplicable( int value ) {
			this.value = value;
		}

		@Override
		public boolean moreApplicableThan( HigherNumberIsMoreApplicable other ) {
			return value > other.value;
		}

	}

	static HigherNumberIsMoreApplicable hip( int value ) {
		return new HigherNumberIsMoreApplicable( value );
	}

	@Test
	public void thatMoreApplicabilityEvalsToTrue() {
		assertTrue( hip( 2 ).moreApplicableThan( hip( 1 ) ) );
	}

	@Test
	public void thatEqualApplicabilityEvalsToFalse() {
		assertFalse( hip( 2 ).moreApplicableThan( hip( 2 ) ) );
	}

	@Test
	public void thatLessApplicabilityEvalsToFalse() {
		assertFalse( hip( 1 ).moreApplicableThan( hip( 2 ) ) );
	}

	@Test
	public void thatMoreApplicabilityComesFirstInSortOrder() {
		HigherNumberIsMoreApplicable[] values = new HigherNumberIsMoreApplicable[] { hip( 1 ), hip( 2 ) };
		Arrays.sort( values, comparator(HigherNumberIsMoreApplicable.class) );
		assertTrue( values[0].value == 2 );
	}

	@Test
	public void thatSameTypeIsNotMoreApplicable() {
		assertNotMoreApplicableThanItself( Type.raw( String.class ) );
	}

	@Test
	public void thatSameDefaultNameIsNotMoreApplicable() {
		assertNotMoreApplicableThanItself( Name.DEFAULT );
	}

	@Test
	public void thatUnnamedIsMoreApplicableThanNamedInstance() {
		Type<Integer> type = raw( Integer.class );
		Instance<Integer> named = instance( named( "foo" ), type );
		Instance<Integer> unnamed = defaultInstanceOf( type );
		assertMoreApplicable( unnamed, named );
	}

	@Test
	public void thatUnnamedIsMoreApplicableThanNamed() {
		assertTrue( Name.DEFAULT.moreApplicableThan( named( "foo" ) ) );
	}

	@Test
	public void thatNamedIsNotMoreApplicableThanUnnamed() {
		assertFalse( named( "bar" ).moreApplicableThan( Name.DEFAULT ) );
	}

	@Test
	public void thatSameSpecificPackageIsNotMoreApplicable() {
		assertNotMoreApplicableThanItself( Packages.packageOf( String.class ) );
	}

	@Test
	public void thatSpecificPackageIsMoreApplicableThanGlobal() {
		assertMoreApplicable( Packages.packageOf( String.class ), Packages.ALL );
	}

	@Test
	public void thatSpecificPackageIsMoreApplicableThanThatPackageWithItsSubPackages() {
		assertMoreApplicable( Packages.packageOf( String.class ),
				Packages.packageAndSubPackagesOf( String.class ) );
	}

	@Test
	public void thatSpecificPackageIsMoreApplicableThanSubPackagesUnderIt() {
		assertMoreApplicable( Packages.packageOf( String.class ),
				Packages.subPackagesOf( String.class ) );
	}

	@Test
	public void thatApplicablityIsGivenByOrdinalStartingWithLowest() {
		DeclarationType[] types = DeclarationType.values();
		for ( int i = 1; i < types.length; i++ ) {
			assertTrue( types[i].moreApplicableThan( types[i - 1] ) );
		}
	}

	@Test
	public void thatExplicitSourceIsMoreApplicableThanAutoSource() {
		Source source = Source.source( TestMorePrecise.class );
		assertMoreApplicable( source.typed( DeclarationType.EXPLICIT ),
				source.typed( DeclarationType.AUTO ) );
	}

	private static <T extends MoreApplicableThan<? super T>> void assertMoreApplicable( T morePrecise,
			T lessPrecise ) {
		assertTrue( morePrecise.moreApplicableThan( lessPrecise ) );
		assertFalse( lessPrecise.moreApplicableThan( morePrecise ) );
	}

	private static <T extends MoreApplicableThan<? super T>> void assertNotMoreApplicableThanItself( T type ) {
		assertFalse( type.moreApplicableThan( type ) );
		assertEquals( 0, Instance.compareApplicability( type, type ) );
	}

	public static <T extends MoreApplicableThan<? super T>> Comparator<T> comparator(@SuppressWarnings("unused") Class<T> cls) {
		return (one, other) ->  Instance.compareApplicability( one, other );
	}

}
