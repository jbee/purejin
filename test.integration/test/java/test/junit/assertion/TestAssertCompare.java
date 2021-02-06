package test.junit.assertion;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import se.jbee.junit.assertion.Assertions;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class TestAssertCompare {

	@Test
	void assertLessThanOrEqual_LessThrows() {
		assertLeThrows(1, 2, "expected: <= <1> but was: <2>");
	}

	@Test
	void assertLessThanOrEqual_LessPositiveThrows() {
		assertLeThrows(-3, 3, "expected: <= <-3> but was: <3>");
	}

	@Test
	void assertLessThan_LessThrows() {
		assertLtThrows(1, 2, "expected: < <1> but was: <2>");
	}

	@Test
	void assertLessThan_EqualThrows() {
		assertLtThrows(12, 12, "expected: < <12> but was: <12>");
	}

	@Test
	void assertLessThan_LessPositiveThrows() {
		assertLtThrows(-3, 3, "expected: < <-3> but was: <3>");
	}

	@Test
	void assertLessThan_EqualNegativeThrows() {
		assertLtThrows(-3, -3, "expected: < <-3> but was: <-3>");
	}

	@Test
	void assertGreaterThanOrEqual_LessThrows() {
		assertGeThrows(2, 1, "expected: >= <2> but was: <1>");
	}

	@Test
	void assertGreaterThanOrEqual_LessNegativeThrows() {
		assertGeThrows(3, -3, "expected: >= <3> but was: <-3>");
	}

	@Test
	void assertGreaterThan_LessThrows() {
		assertGtThrows(2, 1, "expected: > <2> but was: <1>");
	}

	@Test
	void assertGreaterThan_EqualThrows() {
		assertGtThrows(12, 12, "expected: > <12> but was: <12>");
	}

	@Test
	void assertGreaterThan_LessNegativeThrows() {
		assertGtThrows(3, -3, "expected: > <3> but was: <-3>");
	}

	@Test
	void assertGreaterThan_EqualNegativeThrows() {
		assertGtThrows(-3, -3, "expected: > <-3> but was: <-3>");
	}

	private static <T extends Comparable<T>> void assertGeThrows(T expected, T actual, String expectedMessage) {
		assertCompareThrows(
				Assertions::assertGreaterThanOrEqual,
				Assertions::assertGreaterThanOrEqual,
				Assertions::assertGreaterThanOrEqual,
				expected, actual, expectedMessage);
	}

	private static <T extends Comparable<T>> void assertGtThrows(T expected, T actual, String expectedMessage) {
		assertCompareThrows(
				Assertions::assertGreaterThan,
				Assertions::assertGreaterThan,
				Assertions::assertGreaterThan,
				expected, actual, expectedMessage);
	}

	private static <T extends Comparable<T>> void assertLtThrows(T expected, T actual, String expectedMessage) {
		assertCompareThrows(
				Assertions::assertLessThan,
				Assertions::assertLessThan,
				Assertions::assertLessThan,
				expected, actual, expectedMessage);
	}

	private static <T extends Comparable<T>> void assertLeThrows(T expected, T actual, String expectedMessage) {
		assertCompareThrows(
				Assertions::assertLessThanOrEqual,
				Assertions::assertLessThanOrEqual,
				Assertions::assertLessThanOrEqual,
				expected, actual, expectedMessage);
	}

	@FunctionalInterface
	private interface CompareWithMessage<T extends Comparable<T>> {

		void compare(T a, T b, String msg);
	}

	@FunctionalInterface
	private interface CompareWithSuppliedMessage<T extends Comparable<T>> {

		void compare(T a, T b, Supplier<String> msg);
	}

	private static <T extends Comparable<T>> void assertCompareThrows(
			BiConsumer<T, T> cmp0,
			CompareWithMessage<T> cmp1,
			CompareWithSuppliedMessage<T> cmp2,
			T expected, T actual, String expectedMessage) {
		assertNotNull(expected, "expected number should not be null");
		assertNotNull(actual, "actual number should not be null");

		// without message
		AssertionFailedError ex = assertThrows(AssertionFailedError.class, //
				() -> cmp0.accept(expected, actual));
		assertEquals(expectedMessage, ex.getMessage());
		// with null message
		ex = assertThrows(AssertionFailedError.class, //
				() -> cmp1.compare(expected, actual, null));
		assertEquals(expectedMessage, ex.getMessage());
		// with empty message
		ex = assertThrows(AssertionFailedError.class, //
				() -> cmp1.compare(expected, actual, ""));
		assertEquals(expectedMessage, ex.getMessage());
		// with blank message
		ex = assertThrows(AssertionFailedError.class, //
				() -> cmp1.compare(expected, actual, ""));
		// with actual message
		assertEquals(expectedMessage, ex.getMessage());
		ex = assertThrows(AssertionFailedError.class, //
				() -> cmp1.compare(expected, actual, "extra"));
		assertEquals("extra ==> " + expectedMessage, ex.getMessage());
		// null supplier
		ex = assertThrows(AssertionFailedError.class, //
				() -> cmp2.compare(expected, actual, null));
		assertEquals(expectedMessage, ex.getMessage());
		// some supplier
		ex = assertThrows(AssertionFailedError.class, //
				() -> cmp2.compare(expected, actual, () -> "extra"));
		assertEquals("extra ==> " + expectedMessage, ex.getMessage());
	}
}
