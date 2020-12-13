package test.integration.util;

import java.io.*;
import java.util.HashSet;
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

	private static <E> Set<E> set(E[] expected) {
		return new HashSet<>(asList(expected));
	}
}
