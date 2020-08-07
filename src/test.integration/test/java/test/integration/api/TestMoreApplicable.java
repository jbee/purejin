package test.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.raw;

import java.util.Arrays;
import java.util.Comparator;

import org.junit.Test;
import se.jbee.inject.*;
import se.jbee.inject.lang.Qualifying;
import se.jbee.inject.lang.Type;

public class TestMoreApplicable {

	static class HigherNumberIsMoreApplicable
			implements Qualifying<HigherNumberIsMoreApplicable> {

		final int value;

		HigherNumberIsMoreApplicable(int value) {
			this.value = value;
		}

		@Override
		public boolean moreQualifiedThan(HigherNumberIsMoreApplicable other) {
			return value > other.value;
		}

	}

	static HigherNumberIsMoreApplicable hip(int value) {
		return new HigherNumberIsMoreApplicable(value);
	}

	@Test
	public void thatMoreApplicabilityEvalsToTrue() {
		assertTrue(hip(2).moreQualifiedThan(hip(1)));
	}

	@Test
	public void thatEqualApplicabilityEvalsToFalse() {
		assertFalse(hip(2).moreQualifiedThan(hip(2)));
	}

	@Test
	public void thatLessApplicabilityEvalsToFalse() {
		assertFalse(hip(1).moreQualifiedThan(hip(2)));
	}

	@Test
	public void thatMoreApplicabilityComesFirstInSortOrder() {
		HigherNumberIsMoreApplicable[] values = new HigherNumberIsMoreApplicable[] {
				hip(1), hip(2) };
		Arrays.sort(values, comparator(HigherNumberIsMoreApplicable.class));
		assertEquals(2, values[0].value);
	}

	@Test
	public void thatSameTypeIsNotMoreApplicable() {
		assertNotMoreApplicableThanItself(Type.raw(String.class));
	}

	@Test
	public void thatSameDefaultNameIsNotMoreApplicable() {
		assertNotMoreApplicableThanItself(Name.DEFAULT);
	}

	@Test
	public void thatUnnamedIsMoreApplicableThanNamedInstance() {
		Type<Integer> type = raw(Integer.class);
		Instance<Integer> named = instance(named("foo"), type);
		Instance<Integer> unnamed = defaultInstanceOf(type);
		assertMoreApplicable(unnamed, named);
	}

	@Test
	public void thatUnnamedIsMoreApplicableThanNamed() {
		assertTrue(Name.DEFAULT.moreQualifiedThan(named("foo")));
	}

	@Test
	public void thatNamedIsNotMoreApplicableThanUnnamed() {
		assertFalse(named("bar").moreQualifiedThan(Name.DEFAULT));
	}

	@Test
	public void thatSameSpecificPackageIsNotMoreApplicable() {
		assertNotMoreApplicableThanItself(Packages.packageOf(String.class));
	}

	@Test
	public void thatSpecificPackageIsMoreApplicableThanGlobal() {
		assertMoreApplicable(Packages.packageOf(String.class), Packages.ALL);
	}

	@Test
	public void thatSpecificPackageIsMoreApplicableThanThatPackageWithItsSubPackages() {
		assertMoreApplicable(Packages.packageOf(String.class),
				Packages.packageAndSubPackagesOf(String.class));
	}

	@Test
	public void thatSpecificPackageIsMoreApplicableThanSubPackagesUnderIt() {
		assertMoreApplicable(Packages.packageOf(String.class),
				Packages.subPackagesOf(String.class));
	}

	@Test
	public void thatApplicablityIsGivenByOrdinalStartingWithLowest() {
		DeclarationType[] types = DeclarationType.values();
		for (int i = 1; i < types.length; i++) {
			assertTrue(types[i].moreQualifiedThan(types[i - 1]));
		}
	}

	@Test
	public void thatExplicitSourceIsMoreApplicableThanAutoSource() {
		Source source = Source.source(TestMoreApplicable.class);
		assertMoreApplicable(source.typed(DeclarationType.EXPLICIT),
				source.typed(DeclarationType.AUTO));
	}

	private static <T extends Qualifying<? super T>> void assertMoreApplicable(
			T morePrecise, T lessPrecise) {
		assertTrue(morePrecise.moreQualifiedThan(lessPrecise));
		assertFalse(lessPrecise.moreQualifiedThan(morePrecise));
	}

	private static <T extends Qualifying<? super T>> void assertNotMoreApplicableThanItself(
			T type) {
		assertFalse(type.moreQualifiedThan(type));
		assertEquals(0, Qualifying.compare(type, type));
	}

	public static <T extends Qualifying<? super T>> Comparator<T> comparator(
			@SuppressWarnings("unused") Class<T> cls) {
		return (one, other) -> Qualifying.compare(one, other);
	}

}
