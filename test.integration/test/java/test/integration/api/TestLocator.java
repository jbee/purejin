package test.integration.api;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Locator;
import se.jbee.lang.Type;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.defaultInstanceOf;
import static se.jbee.lang.Type.classType;
import static se.jbee.lang.Type.raw;

/**
 * Test to make sure that a {@link Locator} correctly decides whether or not it
 * is usable for a {@link Dependency}.
 * <p>
 * This test (so far) only checks the {@link Type} related aspect as this is the
 * most confusing one that is difficult to do right.
 */
class TestLocator {

	@Test
	void sameTypeIsUsable() {
		assertIsUsable(Integer.class, Integer.class);
	}

	@Test
	void subTypeIsNotUsable() {
		assertIsNotUsable(Integer.class, Number.class);
	}

	@Test
	void superTypeAsUpperBoundIsUsable() {
		assertIsUsable(raw(Integer.class),
				raw(Number.class).asUpperBound());
	}

	@Test
	void superTypeWithWildcardParametersAsUpperBoundIsUsable() {
		assertIsUsable(raw(Integer.class),
				classType(Number.class).asUpperBound());
	}

	@Test
	void sameTypeWithWildcardTypeParameterIsUsable() {
		assertIsUsable(raw(List.class).parameterized(Integer.class),
				raw(List.class).parameterizedAsUpperBounds());
	}

	@Test
	void sameTypeWithSuperUpperBoundTypeParameterIsUsable() {
		assertIsUsable(raw(List.class).parameterized(Integer.class),
				raw(List.class).parameterized(
						raw(Number.class).asUpperBound()));
	}

	@Test
	void sameTypeWithSameTypeUpperBoundTypeParameterIsUsable() {
		assertIsUsable(raw(List.class).parameterized(Integer.class),
				raw(List.class).parameterized(Integer.class).asUpperBound());
	}

	@Test
	void sameTypeAsRawTypeIsUsable() {
		assertIsUsable(raw(List.class).parameterized(Integer.class),
				raw(List.class));
	}

	@Test
	void sameTypeWithIncompatibleUpperBoundTypeParameterIsNotUsable() {
		assertIsNotUsable(raw(List.class).parameterized(Integer.class),
				raw(List.class).parameterized(
						raw(CharSequence.class).asUpperBound()));
	}

	@Test
	void sameTypeWithUpperBoundTypeParameterIsUsable() {
		assertIsUsable(raw(List.class).parameterized(
				raw(Set.class).parameterized(Integer.class)),
				raw(List.class).parameterized(raw(Set.class).asUpperBound()));
	}

	@Test
	void sameTypeWithUpperBoundSuperTypeParameterIsUsable() {
		assertIsUsable(raw(List.class).parameterized(
				raw(TreeSet.class).parameterized(Integer.class)),
				raw(List.class).parameterized(raw(Set.class).asUpperBound()));
	}

	private static void assertIsNotUsable(Class<?> required, Class<?> offered) {
		assertIsNotUsable(raw(required), raw(offered));
	}

	private static void assertIsNotUsable(Type<?> required, Type<?> offered) {
		Locator<?> locator = new Locator<>(defaultInstanceOf(offered));
		Dependency<?> dep = dependency(required);
		assertFalse(locator.isUsableInstanceWise(dep),
				() -> locator + " is wrongly usable for " + dep);
	}

	private static void assertIsUsable(Class<?> required, Class<?> offered) {
		assertIsUsable(raw(required), raw(offered));
	}

	private static void assertIsUsable(Type<?> required, Type<?> offered) {
		Locator<?> locator = new Locator<>(defaultInstanceOf(offered));
		Dependency<?> dep = dependency(required);
		assertTrue(locator.isUsableInstanceWise(dep),
				() -> locator + " is wrongly not usable for " + dep);
	}
}
