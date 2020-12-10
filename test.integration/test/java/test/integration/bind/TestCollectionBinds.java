package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Cast;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.CoreFeature;
import se.jbee.inject.lang.Type;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Cast.*;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.raw;

class TestCollectionBinds {

	private static class CollectionBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("foobar");
			bind(CharSequence.class).to("bar");
			bind(Integer.class).to(42);
			bind(named("foo"), Integer.class).to(846);
			bind(Float.class).to(42.0f);
		}

	}

	private static class CollectionBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			installAll(CoreFeature.class);
			install(CollectionBindsModule.class);
		}

	}

	private static class CollectionBindsJustListBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(CoreFeature.LIST, CoreFeature.COLLECTION);
			install(CollectionBindsModule.class);
		}

	}

	private final Injector injector = Bootstrap.injector(
			CollectionBindsBundle.class);

	@Test
	void thatArrayTypeIsAvailableForAnyBoundType() {
		assertInjects(new String[] { "foobar" }, raw(String[].class));
	}

	@Test
	void thatListIsAvailableForBoundType() {
		assertInjects(singletonList("foobar"), listTypeOf(String.class));
		assertInjects(asList(42, 846), listTypeOf(Integer.class));
	}

	@Test
	void thatSetIsAvailableForBoundType() {
		assertInjects(singleton("foobar"), setTypeOf(String.class));
		assertInjects(new TreeSet<>(asList(new Integer[] { 42, 846 })),
				setTypeOf(Integer.class));
	}

	@Test
	void thatCollectionIsAvailable() {
		Type<? extends Collection<?>> collectionType = Cast.collectionTypeOf(
				Integer.class);
		assertInjectsItems(new Integer[] { 846, 42 }, collectionType);
	}

	@Test
	void thatListAsLowerBoundIsAvailable() {
		Type<? extends List<Number>> wildcardListType = listTypeOf(
				Number.class).parametizedAsUpperBounds();
		assertInjectsItems(new Number[] { 846, 42, 42.0f },
				wildcardListType);
	}

	@Test
	void thatSetAsLowerBoundIsAvailable() {
		Type<? extends Set<Number>> wildcardSetType = setTypeOf(
				Number.class).parametizedAsUpperBounds();
		assertInjectsItems(new Number[] { 846, 42, 42.0f }, wildcardSetType);
	}

	@Test
	void thatCollectionAsLowerBoundIsAvailable() {
		Type<? extends Collection<Number>> collectionType = collectionTypeOf(
				Number.class).parametizedAsUpperBounds();
		assertInjectsItems(new Number[] { 846, 42, 42.0f }, collectionType);
	}

	@Test
	void thatListOfListsOfBoundTypesAreAvailable() {
		assertInjects(singletonList(singletonList("foobar")),
				listTypeOf(listTypeOf(String.class)));
	}

	@Test
	void thatCollectionIsAvailableWhenJustListIsInstalled() {
		Injector injector = Bootstrap.injector(
				CollectionBindsJustListBundle.class);
		assertInjectsItems(new Integer[] { 846, 42 },
				injector.resolve(collectionTypeOf(Integer.class)));
	}

	@Deprecated
	private <E> void assertInjectsItems(E[] expected,
			Type<? extends Collection<?>> dependencyType) {
		assertInjectsItems(asList(expected), dependencyType);
	}

	@Deprecated
	private <E> void assertInjectsItems(Collection<E> expected,
			Type<? extends Collection<?>> dependencyType) {
		assertInjectsItems(expected, injector.resolve(dependencyType));
	}

	@Deprecated
	private <T> void assertInjects(T expected,
			Type<? extends T> dependencyType) {
		assertEqualSets(expected, injector.resolve(dependencyType));
	}

	private static <T> void assertEqualSets(T expected, T actual) {
		if (expected instanceof Object[]) {
			Object[] arr = (Object[]) expected;
			assertArrayEquals(arr, (Object[]) actual);
		} else {
			assertEquals(expected, actual);
		}
	}

	private static <E> void assertInjectsItems(E[] expected, Collection<?> actual) {
		assertInjectsItems(asList(expected), actual);
	}

	private static <E> void assertInjectsItems(Collection<E> expected,
			Collection<?> actual) {
		assertTrue(actual.containsAll(expected));
	}

}
