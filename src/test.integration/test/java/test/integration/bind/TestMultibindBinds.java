package test.integration.bind;

import org.junit.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.CoreFeature;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Cast.listTypeOf;
import static se.jbee.inject.Cast.setTypeOf;
import static se.jbee.inject.Name.named;
import static test.integration.bind.AssertInjects.assertEqualSets;

public class TestMultibindBinds {

	static final Name foo = named("foo");
	static final Name bar = named("bar");

	private static class MultibindBindsModule1 extends BinderModule {

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

	private static class MultibindBindsModule2 extends BinderModule {

		@Override
		protected void declare() {
			multibind(Integer.class).to(11);
			multibind(foo, Integer.class).to(3);
			multibind(bar, Integer.class).to(5);
		}

	}

	private static class MultibindBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(MultibindBindsModule1.class);
			install(MultibindBindsModule2.class);
			install(CoreFeature.SET);
			install(CoreFeature.LIST);
		}

	}

	private final Injector injector = Bootstrap.injector(
			MultibindBindsBundle.class);

	@Test
	public void thatMultipleNamedElementsCanBeBound() {
		Integer[] foos = injector.resolve(foo, Integer[].class);
		assertEqualSets(new Integer[] { 2, 3 }, foos);
		Integer[] bars = injector.resolve(bar, Integer[].class);
		assertEqualSets(new Integer[] { 4, 5 }, bars);
		Integer[] defaults = injector.resolve(Name.DEFAULT, Integer[].class);
		assertEqualSets(new Integer[] { 1, 11 }, defaults);
		Integer[] anys = injector.resolve(Name.ANY, Integer[].class);
		assertEqualSets(new Integer[] { 1, 2, 3, 4, 5, 11 }, anys);
	}

	@Test
	public void thatMultipleBoundNamedElementsCanUsedAsList() {
		List<Integer> foos = injector.resolve(foo, listTypeOf(Integer.class));
		assertEqualSets(new Integer[] { 2, 3 }, foos.toArray());
		List<Integer> bars = injector.resolve(bar, listTypeOf(Integer.class));
		assertEqualSets(new Integer[] { 4, 5 }, bars.toArray());
	}

	@Test
	public void thatMultipleBoundNamedElementsCanUsedAsSet() {
		Set<Integer> foos = injector.resolve(foo, setTypeOf(Integer.class));
		assertEqualSets(new Integer[] { 2, 3 }, foos.toArray());
		Set<Integer> bars = injector.resolve(bar, setTypeOf(Integer.class));
		assertEqualSets(new Integer[] { 4, 5 }, bars.toArray());
	}

	@Test
	public void thatMultipleToConstantsCanBeBound() {
		List<Long> longs = injector.resolve(listTypeOf(long.class));
		assertEqualSets(new Long[] { 1L, 2L, 3L, 4L }, longs.toArray());
		assertEquals(2, injector.resolve(Float[].class).length);
		assertEquals(3, injector.resolve(Double[].class).length);
	}

}
