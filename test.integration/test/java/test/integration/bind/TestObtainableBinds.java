package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Obtainable;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.raw;

/**
 * A test that demonstrates {@link Obtainable} wrapper usage.
 */
class TestObtainableBinds {

	static class Plugin {
		final String name;

		Plugin(String name) {
			this.name = name;
		}
	}

	public static class Manager {

		final Plugin[] plugins;

		public Manager(Obtainable<Plugin[]> plugins) {
			this.plugins = plugins.obtain();
		}
	}

	private static final class TestObtainableBindsModule extends BinderModule {

		@Override
		protected void declare() {
			multibind(named("a"), Plugin.class).to(new Plugin("a"));
			multibind(named("b"), Plugin.class).to(new Plugin("b"));
			multibind(named("c"), Plugin.class).toFactory(context -> {
				throw new UnresolvableDependency.SupplyFailed("Error", new IllegalStateException());
			});
			bind(String.class).to("The name");
			bind(Manager.class).toConstructor();
		}
	}

	private final Injector injector = Bootstrap.injector(TestObtainableBindsModule.class);

	@Test
	public void obtainsArrayElementsThatCouldBeResolved() {
		Manager manager = injector.resolve(Manager.class);
		assertEquals(asList("a", "b"), asList(manager.plugins).stream()
				.map(p -> p.name).collect(toList()));
	}

	@Test
	public void obtainsEmptyArray() {
		assertArrayEquals(new Integer[0], (Integer[]) injector.resolve( //
				raw(Obtainable.class).parametized(Integer[].class)).obtain());
	}

	@Test
	public void obtainsInstanceThatCanBeResolved() {
		assertEquals("The name", injector.resolve( //
				raw(Obtainable.class).parametized(String.class)).obtain());
	}

	@Test
	public void obtainsOrElseForInstanceThatCanNotBeResolved() {
		assertEquals("a default", injector.resolve(named("unbound"), //
				raw(Obtainable.class).parametized(String.class))
				.orElse("a default"));
	}

	@Test
	public void obtainsOriginalExceptionWhenInstanceCanNotBeResolved() {
		Obtainable<?> box = injector.resolve(
				raw(Obtainable.class).parametized(Integer.class));
		try {
			box.orElseThrow();
		} catch (UnresolvableDependency e) {
			assertEquals("No matching resource found.\n"
							+ "\t dependency: java.lang.Integer *\n"
							+ "\tavailable are (for same raw type): none\n"
							+ "\t",
					e.getMessage());
		}
	}
}
