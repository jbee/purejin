package test.junit.assertion;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import se.jbee.junit.assertion.Assertions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class TestAssertCompare {

	/*
	<=
	 */

	@Test
	void assertLessThanOrEqual_EqualPasses() {
		assertLePasses(42, 42);
	}

	@Test
	void assertLessThanOrEqual_LessPasses() {
		assertLePasses(42, 23);
	}

	@Test
	void assertLessThanOrEqual_LessNegativePasses() {
		assertLePasses(42, -23);
	}

	@Test
	void assertLessThanOrEqual_GreaterThrows() {
		assertLeThrows(1, 2, "expected: <= <1> but was: <2>");
	}

	@Test
	void assertLessThanOrEqual_GreaterPositiveThrows() {
		assertLeThrows(-3, 3, "expected: <= <-3> but was: <3>");
	}

	/*
	<
	 */

	@Test
	void assertLessThan_LessPasses() {
		assertLtPasses(41, 13);
	}

	@Test
	void assertLessThan_LessNegativePasses() {
		assertLtPasses(41, -13);
	}

	@Test
	void assertLessThan_GreaterThrows() {
		assertLtThrows(1, 2, "expected: < <1> but was: <2>");
	}

	@Test
	void assertLessThan_EqualThrows() {
		assertLtThrows(12, 12, "expected: < <12> but was: <12>");
	}

	@Test
	void assertLessThan_GreaterPositiveThrows() {
		assertLtThrows(-3, 3, "expected: < <-3> but was: <3>");
	}

	@Test
	void assertLessThan_EqualNegativeThrows() {
		assertLtThrows(-3, -3, "expected: < <-3> but was: <-3>");
	}

	/*
	>=
	 */

	@Test
	void assertGreaterThanOrEqual_GreaterPasses() {
		assertGePasses(2, 5);
	}

	@Test
	void assertGreaterThanOrEqual_EqualPasses() {
		assertGePasses(6, 6);
	}

	@Test
	void assertGreaterThanOrEqual_LessThrows() {
		assertGeThrows(2, 1, "expected: >= <2> but was: <1>");
	}

	@Test
	void assertGreaterThanOrEqual_LessNegativeThrows() {
		assertGeThrows(3, -3, "expected: >= <3> but was: <-3>");
	}

	/*
	>
	 */

	@Test
	void assertGreaterThan_GreaterPasses() {
		assertGtPasses(2, 5);
	}

	@Test
	void assertGreaterThan_GreaterThanNegativePasses() {
		assertGtPasses(-2, 5);
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

	private static <T extends Comparable<T>> void assertGePasses(T expected, T actual) {
		assertComparePasses(
				Assertions::assertGreaterThanOrEqual,
				Assertions::assertGreaterThanOrEqual,
				Assertions::assertGreaterThanOrEqual,
				expected, actual);
	}

	private static <T extends Comparable<T>> void assertGtPasses(T expected, T actual) {
		assertComparePasses(
				Assertions::assertGreaterThan,
				Assertions::assertGreaterThan,
				Assertions::assertGreaterThan,
				expected, actual);
	}

	private static <T extends Comparable<T>> void assertLePasses(T expected, T actual) {
		assertComparePasses(
				Assertions::assertLessThanOrEqual,
				Assertions::assertLessThanOrEqual,
				Assertions::assertLessThanOrEqual,
				expected, actual);
	}

	private static <T extends Comparable<T>> void assertLtPasses(T expected, T actual) {
		assertComparePasses(
				Assertions::assertLessThan,
				Assertions::assertLessThan,
				Assertions::assertLessThan,
				expected, actual);
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

	private static <T extends Comparable<T>> void assertComparePasses(
			BiConsumer<T, T> cmp0,
			CompareWithMessage<T> cmp1,
			CompareWithSuppliedMessage<T> cmp2,
			T expected, T actual) {

		cmp0.accept(expected, actual);
		cmp1.compare(expected, actual, null);
		cmp1.compare(expected, actual, "");
		cmp1.compare(expected, actual, " ");
		cmp2.compare(expected, actual, null);
		AtomicBoolean called = new AtomicBoolean();
		cmp2.compare(expected, actual, () -> {
			called.set(true);
			return "fail";
		});
		assertFalse(called.get(), "supplier was called even though test passed");

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
