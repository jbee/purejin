package se.jbee.junit.assertion;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;

final class AssertCompare {

	private AssertCompare() {
		throw new UnsupportedOperationException("util");
	}

	static <T extends Comparable<T>> void assertGreaterThanOrEqual(T expected, T actual, Supplier<String> message) {
		if (actual.compareTo(expected) < 0)
			failCompare(actual, ">=", expected, message);
	}

	static <T extends Comparable<T>> void assertGreaterThanOrEqual(T expected, T actual, String message) {
		if (actual.compareTo(expected) < 0)
			failCompare(actual, ">=", expected, message);
	}

	static <T extends Comparable<T>> void assertLessThanOrEqual(T expected, T actual, Supplier<String> message) {
		if (actual.compareTo(expected) > 0)
			failCompare(actual, ">=", expected, message);
	}

	static <T extends Comparable<T>> void assertLessThanOrEqual(T expected, T actual, String message) {
		if (actual.compareTo(expected) > 0)
			failCompare(actual, ">=", expected, message);
	}

	static <T extends Comparable<T>> void assertGreaterThan(T expected, T actual, Supplier<String> message) {
		if (actual.compareTo(expected) <= 0)
			failCompare(actual, ">", expected, message);
	}

	static <T extends Comparable<T>> void assertGreaterThan(T expected, T actual, String message) {
		if (actual.compareTo(expected) <= 0)
			failCompare(actual, ">", expected, message);
	}

	static <T extends Comparable<T>> void assertLessThan(T expected, T actual, Supplier<String> message) {
		if (actual.compareTo(expected) >= 0)
			failCompare(actual, ">", expected, message);
	}

	static <T extends Comparable<T>> void assertLessThan(T expected, T actual, String message) {
		if (actual.compareTo(expected) >= 0)
			failCompare(actual, ">", expected, message);
	}

	private static <T extends Comparable<T>> void failCompare(
			T actual, String op, T expected, Supplier<String> message) {
		failCompare(actual, op, expected, message == null ? null : message.get());
	}

	private static <T extends Comparable<T>> void failCompare(
			T actual, String op, T expected, String msg) {
		fail(buildPrefix(msg) + formatCompare(actual, op, expected));
	}

	private static String buildPrefix(String message) {
		return message != null && !message.trim().isEmpty() ? message + " ==> " : "";
	}

	private static <T extends Comparable<T>> String formatCompare(
			T actual, String op, T expected) {
		return String.format("expected: %s <%s> but was: <%s>", //
				op, expected, actual);
	}
}
