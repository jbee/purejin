package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Name.DEFAULT;

/**
 * A test that demonstrates how {@link Bundle}s (here in form of
 * {@link BinderModule}s) are installed for lazy bootstrapped {@link Injector}
 * sub-contexts.
 *
 * This form of {@link Injector} nesting is not limited to any depth. In this
 * example the first level is the "foo" sub-context, the second level is the
 * "bar" sub-context defined in "foo".
 */
class TestExampleInjectorSubContextBinds {

	static final class TestExampleInjectorSubContextBindsModule extends BinderModule {

		@Override
		protected void declare() {
			installIn("foo", SubContextModule1.class);
			installIn("foo", SubContextModule3.class);
		}
	}

	static final class SubContextModule1 extends BinderModule {

		@Override
		protected void declare() {
			bind(int.class).to(42);
		}
	}

	static final class SubContextModule2 extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("42");
		}
	}

	static final class SubContextModule3 extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("13");
			installIn("bar", SubContextModule2.class);
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestExampleInjectorSubContextBindsModule.class);

	@Test
	void bindsInstalledInSubContextAreNotAccessibleInParent() {
		assertThrows(UnresolvableDependency.class,
				() -> injector.resolve(int.class));
	}

	@Test
	void bindsAreAvailableInTheSubContextTheyWereInstalledIn() {
		Injector foo = injector.subContext("foo");
		assertNotSame(foo, injector);
		assertEquals(42, foo.resolve(int.class).intValue());
		assertEquals("13", foo.resolve(String.class));
		Injector bar = foo.subContext("bar");
		assertNotSame(bar, injector);
		assertNotSame(foo, bar);
		assertEquals("42", bar.resolve(String.class));
	}

	@Test
	void anySubContextCanBeResolvedButItMightBeEmpty() {
		Injector subContext = injector.subContext("withoutInstallIn");
		assertNotNull(subContext);
		Exception ex = assertThrows(UnresolvableDependency.class,
				() -> subContext.resolve(Injector.class),
				"A non existing sub context shouldn't be able to resolve anything");
		assertTrue(ex.getMessage().contains("Empty SubContext Injector"));
	}

	@Test
	void subContextEnvironmentIsSameAsRootContexts() {
		Injector foo = injector.subContext("foo");
		assertNotSame(foo, injector);
		assertSame(injector.resolve(DEFAULT, Env.class),
				foo.resolve(DEFAULT, Env.class));
	}

}
