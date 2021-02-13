package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.lang.Qualifying;
import se.jbee.lang.Type;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;

class TestMoreQualified {

	private static class HigherNumberIsMoreQualified
			implements Qualifying<HigherNumberIsMoreQualified> {

		final int value;

		HigherNumberIsMoreQualified(int value) {
			this.value = value;
		}

		@Override
		public boolean moreQualifiedThan(HigherNumberIsMoreQualified other) {
			return value > other.value;
		}

	}

	static HigherNumberIsMoreQualified hip(int value) {
		return new HigherNumberIsMoreQualified(value);
	}

	@Test
	void moreQualifiedValueIsRecognisedAsMoreQualified() {
		assertTrue(hip(2).moreQualifiedThan(hip(1)));
	}

	@Test
	void equallyQualifiedValueIsNotMoreQualified() {
		assertFalse(hip(2).moreQualifiedThan(hip(2)));
	}

	@Test
	void lessQualifiedValueIsNotMoreQualified() {
		assertFalse(hip(1).moreQualifiedThan(hip(2)));
	}

	@Test
	void mostQualifiedIsFirstInSortOrder() {
		HigherNumberIsMoreQualified[] values = new HigherNumberIsMoreQualified[] {
				hip(1), hip(2) };
		Arrays.sort(values, Qualifying::compare);
		assertEquals(2, values[0].value);
	}

	@Test
	void sameTypeIsNotMoreQualified() {
		assertNotMoreQualifiedThanItself(Type.raw(String.class));
	}

	@Test
	void sameDefaultNameIsNotMoreQualified() {
		assertNotMoreQualifiedThanItself(Name.DEFAULT);
	}

	/**
	 * This might sound a bit surprising at first. The reason is that an
	 * "unnamed" instance is actually a named instance having the {@link
	 * Name#DEFAULT} name. This is so that in case such a instance exists and
	 * {@link Name#ANY} is used to resolve it you get the default named
	 * instance. Should on the other hand a specific name be resolved it does
	 * not matter that the default name is more qualified as it will not match
	 * the specific name resolved.
	 */
	@Test
	void unnamedIsMoreQualifiedThanNamedInstance() {
		Type<Integer> type = raw(Integer.class);
		Instance<Integer> named = instance(named("foo"), type);
		Instance<Integer> unnamed = defaultInstanceOf(type);
		assertMoreQualified(unnamed, named);
	}

	@Test
	void unnamedIsMoreQualifiedThanNamed() {
		assertTrue(Name.DEFAULT.moreQualifiedThan(named("foo")));
	}

	@Test
	void namedIsNotMoreQualifiedThanUnnamed() {
		assertFalse(named("bar").moreQualifiedThan(Name.DEFAULT));
	}

	@Test
	void sameSpecificPackageIsNotMoreQualified() {
		assertNotMoreQualifiedThanItself(Packages.packageOf(String.class));
	}

	@Test
	void specificPackageIsMoreQualifiedThanGlobal() {
		assertMoreQualified(Packages.packageOf(String.class), Packages.ALL);
	}

	@Test
	void specificPackageIsMoreQualifiedThanThatPackageWithItsSubPackages() {
		assertMoreQualified(Packages.packageOf(String.class),
				Packages.packageAndSubPackagesOf(String.class));
	}

	@Test
	void specificPackageIsMoreQualifiedThanSubPackagesUnderIt() {
		assertMoreQualified(Packages.packageOf(String.class),
				Packages.subPackagesOf(String.class));
	}

	@Test
	void qualificationIsGivenByOrdinalStartingWithLowest() {
		DeclarationType[] types = DeclarationType.values();
		for (int i = 1; i < types.length; i++) {
			assertTrue(types[i].moreQualifiedThan(types[i - 1]));
		}
	}

	@Test
	void explicitSourceIsMoreQualifiedThanPublishedSource() {
		Source source = Source.source(TestMoreQualified.class);
		assertMoreQualified(source.typed(DeclarationType.EXPLICIT),
				source.typed(DeclarationType.PUBLISHED));
	}

	private static <T extends Qualifying<? super T>> void assertMoreQualified(
			T morePrecise, T lessPrecise) {
		assertTrue(morePrecise.moreQualifiedThan(lessPrecise));
		assertFalse(lessPrecise.moreQualifiedThan(morePrecise));
	}

	private static <T extends Qualifying<? super T>> void assertNotMoreQualifiedThanItself(
			T type) {
		assertFalse(type.moreQualifiedThan(type));
		assertEquals(0, Qualifying.compare(type, type));
	}

}
