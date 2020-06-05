package se.jbee.inject.bind;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import se.jbee.inject.Injector;
import se.jbee.inject.Type;

public class AssertInjects {

	private final Injector injector;

	public AssertInjects(Injector injector) {
		this.injector = injector;
	}

	public <T> void assertInjects(T expected,
			Type<? extends T> dependencyType) {
		if (expected instanceof Object[]) {
			Object[] arr = (Object[]) expected;
			assertArrayEquals(arr, (Object[]) injector.resolve(dependencyType));
		} else {
			assertEquals(expected, injector.resolve(dependencyType));
		}
	}

	public <E> void assertInjectsItems(E[] expected,
			Type<? extends Collection<?>> dependencyType) {
		assertInjectsItems(asList(expected), dependencyType);
	}

	public <E> void assertInjectsItems(Collection<E> expected,
			Type<? extends Collection<?>> dependencyType) {
		assertTrue(injector.resolve(dependencyType).containsAll(expected));
	}

	public static <E> void assertEqualSets(E[] expected, E[] actual) {
		assertEquals(expected.length, actual.length);
		assertEquals(set(expected), set(actual));
	}

	private static <E> Set<E> set(E[] expected) {
		return new HashSet<>(Arrays.asList(expected));
	}
}
