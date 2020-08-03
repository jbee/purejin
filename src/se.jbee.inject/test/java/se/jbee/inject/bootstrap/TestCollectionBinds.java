package se.jbee.inject.bootstrap;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static se.jbee.inject.Cast.collectionTypeOf;
import static se.jbee.inject.Cast.listTypeOf;
import static se.jbee.inject.Cast.setTypeOf;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import se.jbee.inject.Cast;
import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.CoreFeature;

public class TestCollectionBinds {

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
	private final AssertInjects ai = new AssertInjects(injector);

	@Test
	public void thatArrayTypeIsAvailableForAnyBoundType() {
		ai.assertInjects(new String[] { "foobar" }, raw(String[].class));
	}

	@Test
	public void thatListIsAvailableForBoundType() {
		ai.assertInjects(singletonList("foobar"), listTypeOf(String.class));
		ai.assertInjects(asList(42, 846), listTypeOf(Integer.class));
	}

	@Test
	public void thatSetIsAvailableForBoundType() {
		ai.assertInjects(singleton("foobar"), setTypeOf(String.class));
		ai.assertInjects(new TreeSet<>(asList(new Integer[] { 42, 846 })),
				setTypeOf(Integer.class));
	}

	@Test
	public void thatCollectionIsAvailable() {
		Type<? extends Collection<?>> collectionType = Cast.collectionTypeOf(
				Integer.class);
		ai.assertInjectsItems(new Integer[] { 846, 42 }, collectionType);
	}

	@Test
	public void thatListAsLowerBoundIsAvailable() {
		Type<? extends List<Number>> wildcardListType = listTypeOf(
				Number.class).parametizedAsUpperBounds();
		ai.assertInjectsItems(new Number[] { 846, 42, 42.0f },
				wildcardListType);
	}

	@Test
	public void thatSetAsLowerBoundIsAvailable() {
		Type<? extends Set<Number>> wildcardSetType = setTypeOf(
				Number.class).parametizedAsUpperBounds();
		ai.assertInjectsItems(new Number[] { 846, 42, 42.0f }, wildcardSetType);
	}

	@Test
	public void thatCollectionAsLowerBoundIsAvailable() {
		Type<? extends Collection<Number>> collectionType = collectionTypeOf(
				Number.class).parametizedAsUpperBounds();
		ai.assertInjectsItems(new Number[] { 846, 42, 42.0f }, collectionType);
	}

	@Test
	public void thatListOfListsOfBoundTypesAreAvailable() {
		ai.assertInjects(singletonList(singletonList("foobar")),
				listTypeOf(listTypeOf(String.class)));
	}

	@Test
	public void thatCollectionIsAvailableWhenJustListIsInstalled() {
		Injector injector = Bootstrap.injector(
				CollectionBindsJustListBundle.class);
		Type<? extends Collection<?>> collectionType = collectionTypeOf(
				Integer.class);
		new AssertInjects(injector).assertInjectsItems(
				new Integer[] { 846, 42 }, collectionType);
	}
}
