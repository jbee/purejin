package se.jbee.junit.assertion;

import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public final class Assertions {

	private Assertions() {
		throw new UnsupportedOperationException("util");
	}

	/*
	Comparable >, >=, <, <= checks
	 */

	public static <T extends Comparable<T>> void assertGreaterThanOrEqual(T expected, T actual) {
		AssertCompare.assertGreaterThanOrEqual(expected, actual, (String) null);
	}

	public static <T extends Comparable<T>> void assertGreaterThanOrEqual(T expected, T actual, String message) {
		AssertCompare.assertGreaterThanOrEqual(expected, actual, message);
	}

	public static <T extends Comparable<T>> void assertGreaterThanOrEqual(T expected, T actual, Supplier<String> message) {
		AssertCompare.assertGreaterThanOrEqual(expected, actual, message);
	}

	public static void assertGreaterThanOrEqual(int expected, int actual) {
		AssertCompare.assertGreaterThanOrEqual(expected, actual, (String) null);
	}

	public static void assertGreaterThanOrEqual(int expected, int actual, String message) {
		AssertCompare.assertGreaterThanOrEqual(expected, actual, message);
	}

	public static void assertGreaterThanOrEqual(int expected, int actual, Supplier<String> message) {
		AssertCompare.assertGreaterThanOrEqual(expected, actual, message);
	}

	public static void assertGreaterThanOrEqual(long expected, long actual) {
		AssertCompare.assertGreaterThanOrEqual(expected, actual, (String) null);
	}

	public static void assertGreaterThanOrEqual(long expected, long actual, String message) {
		AssertCompare.assertGreaterThanOrEqual(expected, actual, message);
	}

	public static void assertGreaterThanOrEqual(long expected, long actual, Supplier<String> message) {
		AssertCompare.assertGreaterThanOrEqual(expected, actual, message);
	}

	public static <T extends Comparable<T>> void assertGreaterThan(T expected, T actual) {
		AssertCompare.assertGreaterThan(expected, actual, (String) null);
	}

	public static <T extends Comparable<T>> void assertGreaterThan(T expected, T actual, String message) {
		AssertCompare.assertGreaterThan(expected, actual, message);
	}

	public static <T extends Comparable<T>> void assertGreaterThan(T expected, T actual, Supplier<String> message) {
		AssertCompare.assertGreaterThan(expected, actual, message);
	}

	public static void assertGreaterThan(int expected, int actual) {
		AssertCompare.assertGreaterThan(expected, actual, (String) null);
	}

	public static void assertGreaterThan(int expected, int actual, String message) {
		AssertCompare.assertGreaterThan(expected, actual, message);
	}

	public static void assertGreaterThan(int expected, int actual, Supplier<String> message) {
		AssertCompare.assertGreaterThan(expected, actual, message);
	}

	public static void assertGreaterThan(long expected, long actual) {
		AssertCompare.assertGreaterThan(expected, actual, (String) null);
	}

	public static void assertGreaterThan(long expected, long actual, String message) {
		AssertCompare.assertGreaterThan(expected, actual, message);
	}

	public static void assertGreaterThan(long expected, long actual, Supplier<String> message) {
		AssertCompare.assertGreaterThan(expected, actual, message);
	}

	public static <T extends Comparable<T>> void assertLessThan(T expected, T actual) {
		AssertCompare.assertLessThan(expected, actual, (String) null);
	}

	public static <T extends Comparable<T>> void assertLessThan(T expected, T actual, String message) {
		AssertCompare.assertLessThan(expected, actual, message);
	}

	public static <T extends Comparable<T>> void assertLessThan(T expected, T actual, Supplier<String> message) {
		AssertCompare.assertLessThan(expected, actual, message);
	}

	public static void assertLessThan(int expected, int actual) {
		AssertCompare.assertLessThan(expected, actual, (String) null);
	}

	public static void assertLessThan(int expected, int actual, String message) {
		AssertCompare.assertLessThan(expected, actual, message);
	}

	public static void assertLessThan(int expected, int actual, Supplier<String> message) {
		AssertCompare.assertLessThan(expected, actual, message);
	}

	public static void assertLessThan(long expected, long actual) {
		AssertCompare.assertLessThan(expected, actual, (String) null);
	}

	public static void assertLessThan(long expected, long actual, String message) {
		AssertCompare.assertLessThan(expected, actual, message);
	}

	public static void assertLessThan(long expected, long actual, Supplier<String> message) {
		AssertCompare.assertLessThan(expected, actual, message);
	}

	public static <T extends Comparable<T>> void assertLessThanOrEqual(T expected, T actual) {
		AssertCompare.assertLessThanOrEqual(expected, actual, (String) null);
	}

	public static <T extends Comparable<T>> void assertLessThanOrEqual(T expected, T actual, String message) {
		AssertCompare.assertLessThanOrEqual(expected, actual, message);
	}

	public static <T extends Comparable<T>> void assertLessThanOrEqual(T expected, T actual, Supplier<String> message) {
		AssertCompare.assertLessThanOrEqual(expected, actual, message);
	}

	public static void assertLessThanOrEqual(int expected, int actual) {
		AssertCompare.assertLessThanOrEqual(expected, actual, (String) null);
	}

	public static void assertLessThanOrEqual(int expected, int actual, String message) {
		AssertCompare.assertLessThanOrEqual(expected, actual, message);
	}

	public static void assertLessThanOrEqual(int expected, int actual, Supplier<String> message) {
		AssertCompare.assertLessThanOrEqual(expected, actual, message);
	}

	public static void assertLessThanOrEqual(long expected, long actual) {
		AssertCompare.assertLessThanOrEqual(expected, actual, (String) null);
	}

	public static void assertLessThanOrEqual(long expected, long actual, String message) {
		AssertCompare.assertLessThanOrEqual(expected, actual, message);
	}

	public static void assertLessThanOrEqual(long expected, long actual, Supplier<String> message) {
		AssertCompare.assertLessThanOrEqual(expected, actual, message);
	}

	/*
	Await (asynchronous assertions)
	 */

	public static void assertAwait(Await await, Callable<AssertionFailedError> test) {
		AssertAwait.assertAwait(await, test);
	}

	public static void assertAwait(Await await, Executable test) {
		AssertAwait.assertAwait(await, test);
	}

	/*
	Equal-ish asserts mostly for collections
	 */

	public static void assertToStringEquals(String expected, Map<?, ?> actual) {
		AssertEqualIsh.assertToStringEquals(expected, actual);
	}

	public static <E> void assertAllSame(Collection<E> actual) {
		AssertEqualIsh.assertAllSame(actual);
	}

	public static <E> void assertEqualsIgnoreOrder(E[] expected, E[] actual) {
		AssertEqualIsh.assertEqualsIgnoreOrder(expected, actual);
	}

}
