package se.jbee.junit.assertion;

import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class AssertEqualIsh {

	private AssertEqualIsh() {
		throw new UnsupportedOperationException("util");
	}

	static <E> void assertAllSame(Collection<E> actual) {
		if (actual.size() <= 1)
			return;
		E e1 = actual.iterator().next();
		for (E e : actual)
			assertSame(e1, e);
	}

	static <E> void assertEqualsIgnoreOrder(E[] expected, E[] actual) {
		assertEquals(expected.length, actual.length,
				() ->  "expected " + Arrays.toString(
						expected) + " but got: " + Arrays.toString(actual));
		assertEquals(new HashSet<>(asList(expected)),
				new HashSet<>(asList(actual)));
	}

	/**
	 * Compares an actual {@link Map} with an expected {@link Object#toString()}
	 * in a way that does not expect the actual {@link Map} to have the same
	 * entry order. Or in other words entry order does not matter even though
	 * the expected {@link String} does present the entries in some particular
	 * order.
	 *
	 * @param expectedToString the expected {@link Object#toString()} output of
	 *                         the map assuming the keys would be ordered in
	 *                         just the order given here
	 * @param actual           actual map (that does not need to have the same
	 *                         key order)
	 */
	static void assertToStringEquals(String expectedToString, Map<?, ?> actual) {
		expectedToString = expectedToString.substring(1, expectedToString.length()-1); // cut off { }
		String[] pairs = expectedToString.split(", ");
		Map<String, String> expectedMap = new HashMap<>();
		for (String pair : pairs) {
			String[] keyValue = pair.split("=");
			expectedMap.put(keyValue[0], keyValue[1]);
		}
		assertEquals(expectedMap.size(), actual.size());
		for (Map.Entry<?, ?> e : actual.entrySet()) {
			String actualKey = String.valueOf(e.getKey());
			String actualValue = String.valueOf(e.getValue());
			assertEquals(expectedMap.get(actualKey), actualValue,
					"expected entries `" + expectedToString + "` but got: `" + actual.toString() + "`");
		}
	}
}
