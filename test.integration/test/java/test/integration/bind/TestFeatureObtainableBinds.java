package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Obtainable;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.Arrays;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Obtainable.obtainableTypeOf;

/**
 * A test that demonstrates {@link Obtainable} wrapper usage.
 */
class TestFeatureObtainableBinds {

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
			multibind("a", Plugin.class).to(new Plugin("a"));
			multibind("b", Plugin.class).to(new Plugin("b"));
			multibind("c", Plugin.class).toFactory(context -> {
				throw new UnresolvableDependency.SupplyFailed("Error", new IllegalStateException());
			});
			bind(String.class).to("The name");
			bind(Manager.class).toConstructor();
		}
	}

	private final Injector context = Bootstrap.injector(
			TestObtainableBindsModule.class);

	@Test
	void obtainsArrayElementsThatCouldBeResolved() {
		Manager manager = context.resolve(Manager.class);
		assertEquals(asList("a", "b"), Arrays.stream(manager.plugins)
				.map(p -> p.name).collect(toList()));
	}

	@Test
	void obtainsEmptyArray() {
		assertArrayEquals(new Integer[0], context.resolve( //
				obtainableTypeOf(Integer[].class)).obtain());
	}

	@Test
	void obtainsInstanceThatCanBeResolved() {
		assertEquals("The name", context.resolve( //
				obtainableTypeOf(String.class)).obtain());
	}

	@Test
	void obtainsOrElseForInstanceThatCanNotBeResolved() {
		assertEquals("a default", context.resolve(named("unbound"), //
				obtainableTypeOf(String.class)).orElse("a default"));
	}

	@Test
	void obtainsOriginalExceptionWhenInstanceCanNotBeResolved() {
		Obtainable<?> box = context.resolve(obtainableTypeOf(Integer.class));
		Exception ex = assertThrows(UnresolvableDependency.class,
				box::orElseThrow);
		assertEquals(
				"No matching resource found.\n"
						+ "\t dependency: java.lang.Integer *\n"
						+ "\tavailable are (for same raw type): none",
				ex.getMessage());
	}
}
