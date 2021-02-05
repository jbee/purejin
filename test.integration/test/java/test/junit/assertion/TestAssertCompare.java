package test.junit.assertion;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import se.jbee.junit.assertion.Assertions;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class TestAssertCompare {

	@Test
	void assertGreaterThanOrEqual_Less() {
		assertCompareThrows(
				Assertions::assertGreaterThanOrEqual,
				Assertions::assertGreaterThanOrEqual,
				2, 1, "expected: >= <2> but was: <1>");
	}

	@Test
	void assertGreaterThanOrEqual_LessNegative() {
		assertCompareThrows(
				Assertions::assertGreaterThanOrEqual,
				Assertions::assertGreaterThanOrEqual,
				2, -2, "expected: >= <2> but was: <-2>");
	}

	@FunctionalInterface
	private interface CompareWithMessage<T extends Comparable<T>> {

		void compare(T a, T b, String msg);
	}

	@FunctionalInterface
	private interface CompareWithSuppliedMessage<T extends Comparable<T>> {

		void compare(T a, T b, Supplier<String> msg);
	}

	private <T extends Comparable<T>> void assertCompareThrows(
			CompareWithMessage<T> cmp1, CompareWithSuppliedMessage<T> cmp2,
			T expected, T actual, String expectedMessage) {
		assertNotNull(expected, "expected number should not be null");
		assertNotNull(actual, "actual number should not be null");

		// with null message
		AssertionFailedError ex = assertThrows(AssertionFailedError.class, //
				() -> cmp1.compare(expected, actual, null));
		assertEquals(expectedMessage, ex.getMessage());
		// with empty message
		ex = assertThrows(AssertionFailedError.class, //
				() -> cmp1.compare(expected, actual, ""));
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
