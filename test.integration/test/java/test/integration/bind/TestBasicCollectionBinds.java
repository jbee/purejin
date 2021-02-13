package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.DefaultFeature;
import se.jbee.lang.Cast;
import se.jbee.lang.Type;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Cast.*;
import static se.jbee.lang.Type.raw;

class TestBasicCollectionBinds {

	private static class TestBasicCollectionBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("foobar");
			bind(CharSequence.class).to("bar");
			bind(Integer.class).to(42);
			bind(named("foo"), Integer.class).to(846);
			bind(Float.class).to(42.0f);
		}
	}

	private static class AllFeaturesBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			installAll(DefaultFeature.class);
			install(TestBasicCollectionBindsModule.class);
		}
	}

	private static class JustListFeatureBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(DefaultFeature.LIST, DefaultFeature.COLLECTION);
			install(TestBasicCollectionBindsModule.class);
		}
	}

	private final Injector injector = Bootstrap.injector(
			AllFeaturesBundle.class);

	@Test
	void arrayTypeIsAvailableForAnyBoundType() {
		assertInjects(new String[] { "foobar" }, raw(String[].class));
	}

	@Test
	void listIsAvailableForBoundType() {
		assertInjects(singletonList("foobar"), listTypeOf(String.class));
		assertInjects(asList(42, 846), listTypeOf(Integer.class));
	}

	@Test
	void setIsAvailableForBoundType() {
		assertInjects(singleton("foobar"), setTypeOf(String.class));
		assertInjects(new TreeSet<>(asList(42, 846)),
				setTypeOf(Integer.class));
	}

	@Test
	void collectionIsAvailable() {
		Type<? extends Collection<?>> collectionType = Cast.collectionTypeOf(
				Integer.class);
		assertInjectsItems(new Integer[] { 846, 42 }, collectionType);
	}

	@Test
	void listAsLowerBoundIsAvailable() {
		Type<? extends List<Number>> wildcardListType = listTypeOf(
				Number.class).parameterizedAsUpperBounds();
		assertInjectsItems(new Number[] { 846, 42, 42.0f }, wildcardListType);
	}

	@Test
	void setAsLowerBoundIsAvailable() {
		Type<? extends Set<Number>> wildcardSetType = setTypeOf(
				Number.class).parameterizedAsUpperBounds();
		assertInjectsItems(new Number[] { 846, 42, 42.0f }, wildcardSetType);
	}

	@Test
	void collectionAsLowerBoundIsAvailable() {
		Type<? extends Collection<Number>> collectionType = collectionTypeOf(
				Number.class).parameterizedAsUpperBounds();
		assertInjectsItems(new Number[] { 846, 42, 42.0f }, collectionType);
	}

	@Test
	void listOfListsOfBoundTypesAreAvailable() {
		assertInjects(singletonList(singletonList("foobar")),
				listTypeOf(listTypeOf(String.class)));
	}

	@Test
	void collectionIsAvailableWhenJustListIsInstalled() {
		Injector injector = Bootstrap.injector(JustListFeatureBundle.class);
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

	private static <E> void assertInjectsItems(E[] expected,
			Collection<?> actual) {
		assertInjectsItems(asList(expected), actual);
	}

	private static <E> void assertInjectsItems(Collection<E> expected,
			Collection<?> actual) {
		assertTrue(actual.containsAll(expected));
	}

}
