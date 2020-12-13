package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.CoreFeature;
import se.jbee.inject.lang.Type;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Cast.listTypeOf;
import static se.jbee.inject.lang.Cast.setTypeOf;
import static test.integration.util.TestUtils.assertEqualSets;

/**
 * A {@link se.jbee.inject.binder.Binder.TypedBinder#multibind(Type)} is a
 * normal bind except that it declares that further binds for the same {@link
 * Name} and {@link Type}, in other words for the same {@link
 * se.jbee.inject.Instance} are valid. In such cases the receiver usually
 * expects a collection of matching dependencies and all that have been bound
 * with {@link se.jbee.inject.DeclarationType#MULTI} are injected.
 * <p>
 * If such binds would instead use a normal {@link se.jbee.inject.binder.Binder.TypedBinder#bind(Type)},
 * which makes them a {@link se.jbee.inject.DeclarationType#EXPLICIT} bind,
 * bindings of the same {@link Name} and {@link Type} do clash with each other
 * and cause an exception during bootstrapping of the {@link Injector} context.
 */
class TestBasicMultibindBinds {

	static final Name foo = named("foo");
	static final Name bar = named("bar");

	private static class TestBasicMultibindBindsModule1 extends BinderModule {

		@Override
		protected void declare() {
			multibind(Integer.class).to(1);
			multibind(foo, Integer.class).to(2);
			multibind(bar, Integer.class).to(4);
			bind(Long.class).to(1L, 2L, 3L, 4L);
			bind(Float.class).to(2f, 3f);
			bind(Double.class).to(5d, 6d, 7d);
		}

	}

	private static class TestBasicMultibindBindsModule2 extends BinderModule {

		@Override
		protected void declare() {
			multibind(Integer.class).to(11);
			multibind(foo, Integer.class).to(3);
			multibind(bar, Integer.class).to(5);
		}

	}

	private static class TestBasicMultibindBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(TestBasicMultibindBindsModule1.class);
			install(TestBasicMultibindBindsModule2.class);
			install(CoreFeature.SET);
			install(CoreFeature.LIST);
		}

	}

	private final Injector context = Bootstrap.injector(
			TestBasicMultibindBindsBundle.class);

	@Test
	void multipleNamedElementsCanBeBound() {
		Integer[] foos = context.resolve(foo, Integer[].class);
		assertEqualSets(new Integer[] { 2, 3 }, foos);
		Integer[] bars = context.resolve(bar, Integer[].class);
		assertEqualSets(new Integer[] { 4, 5 }, bars);
		Integer[] defaults = context.resolve(Name.DEFAULT, Integer[].class);
		assertEqualSets(new Integer[] { 1, 11 }, defaults);
		Integer[] anys = context.resolve(Name.ANY, Integer[].class);
		assertEqualSets(new Integer[] { 1, 2, 3, 4, 5, 11 }, anys);
	}

	@Test
	void multipleBoundNamedElementsCanUsedAsList() {
		List<Integer> foos = context.resolve(foo, listTypeOf(Integer.class));
		assertEqualSets(new Integer[] { 2, 3 }, foos.toArray());
		List<Integer> bars = context.resolve(bar, listTypeOf(Integer.class));
		assertEqualSets(new Integer[] { 4, 5 }, bars.toArray());
	}

	@Test
	void multipleBoundNamedElementsCanUsedAsSet() {
		Set<Integer> foos = context.resolve(foo, setTypeOf(Integer.class));
		assertEqualSets(new Integer[] { 2, 3 }, foos.toArray());
		Set<Integer> bars = context.resolve(bar, setTypeOf(Integer.class));
		assertEqualSets(new Integer[] { 4, 5 }, bars.toArray());
	}

	@Test
	void multipleToConstantsCanBeBound() {
		List<Long> longs = context.resolve(listTypeOf(long.class));
		assertEqualSets(new Long[] { 1L, 2L, 3L, 4L }, longs.toArray());
		assertEquals(2, context.resolve(Float[].class).length);
		assertEquals(3, context.resolve(Double[].class).length);
	}
}
