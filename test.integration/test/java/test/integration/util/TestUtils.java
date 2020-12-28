package test.integration.util;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public final class TestUtils {

	private TestUtils() {
		throw new UnsupportedOperationException("util");
	}

	public static boolean wait50() {
		return await(50);
	}

	public static boolean wait20() {
		return await(20);
	}

	@SuppressWarnings("squid:S2925")
	public static boolean await(long millis) {
		try {
			Thread.sleep(millis);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}

	public static void assertSerializable(Serializable obj) {
		try {
			byte[] binObj = serialize(obj);
			Serializable obj2 = deserialize(binObj);
			assertEquals(obj, obj2);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	public static byte[] serialize(Serializable obj) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(obj);
		oos.close();
		return baos.toByteArray();
	}

	public static Serializable deserialize(byte[] b)
			throws IOException, ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		ObjectInputStream ois = new ObjectInputStream(bais);
		return (Serializable) ois.readObject();
	}

	public static <E> void assertEqualSets(E[] expected, E[] actual) {
		assertEquals(expected.length, actual.length);
		assertEquals(set(expected), set(actual));
	}

	/**
	 * Compares an actual {@link Map} with an expected {@link Object#toString()}
	 * in a way that does not expect the actual {@link Map} to have the same
	 * entry order. Or in other words entry order does not matter even though
	 * the expected {@link String} does present the entries in some particular
	 * order.
	 *
	 * @param expected the expected {@link Object#toString()} output of the map
	 *                 assuming the keys would be ordered in just the order
	 *                 given here
	 * @param actual   actual map (that does not need to have the same key
	 *                 order)
	 */
	public static void assertEqualMaps(String expected, Map<?, ?> actual) {
		expected = expected.substring(1, expected.length()-1); // cut off { }
		String[] pairs = expected.split(", ");
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
					"expected entries `" + expected + "` but got: `" + actual.toString() + "`");
		}
	}

	private static <E> Set<E> set(E[] expected) {
		return new HashSet<>(asList(expected));
	}
}
