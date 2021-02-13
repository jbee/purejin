package se.jbee.junit.assertion;

final class AssertionUtils {

	private AssertionUtils() {
		throw new UnsupportedOperationException("util");
	}

	static String buildPrefix(String message) {
		return message != null && !message.trim().isEmpty() ? message + " ==> " : "";
	}

	static <T extends Comparable<T>> String formatExpectedButWas(
			String op, T expected, T butWas) {
		return String.format("expected: %s <%s> but was: <%s>", //
				op, expected, butWas);
	}
}
