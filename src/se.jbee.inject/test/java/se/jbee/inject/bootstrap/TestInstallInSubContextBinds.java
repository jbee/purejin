package se.jbee.inject.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.jbee.inject.Name.DEFAULT;
import static se.jbee.inject.Type.raw;

import org.junit.Test;

import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * A test that demonstrates how {@link Bundle}s (here in form of
 * {@link BinderModule}s) are installed for lazy bootstrapped {@link Injector}
 * sub-contexts.
 * 
 * This form of {@link Injector} nesting is not limited to any depth. In this
 * example the first level is the "foo" sub-context, the second level is the
 * "bar" sub-context defined in "foo".
 */
public class TestInstallInSubContextBinds {

	static final class TestInstallInSubContextBindsModule extends BinderModule {

		@Override
		protected void declare() {
			installIn("foo", LazyModule1.class);
			installIn("foo", LazyModule3.class);
		}

	}

	static final class LazyModule1 extends BinderModule {

		@Override
		protected void declare() {
			bind(int.class).to(42);
		}

	}

	static final class LazyModule2 extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("42");
		}

	}

	static final class LazyModule3 extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("13");
			installIn("bar", LazyModule2.class);
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestInstallInSubContextBindsModule.class);

	@Test(expected = UnresolvableDependency.class)
	public void bindsInstalledInSubContextAreNotAccessibleInParent() {
		injector.resolve(int.class);
	}

	@Test
	public void bindsAreAvailableInTheSubContextTheyWereInstalledIn() {
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
	public void anySubContextCanBeResolvedButItMightBeEmpty() {
		Injector subContext = injector.subContext("withoutIntallIn");
		assertNotNull(subContext);
		try {
			subContext.resolve(Injector.class);
			fail("A non existing sub context shoudn't be able to resolve anything");
		} catch (UnresolvableDependency e) {
			assertTrue(e.getMessage().contains("Empty SubContext Injector"));
		}
	}

	@Test
	public void subContextEnvironmentIsSameAsRootContexts() {
		Injector foo = injector.subContext("foo");
		assertNotSame(foo, injector);
		assertSame(injector.resolve(DEFAULT, raw(Env.class)),
				foo.resolve(DEFAULT, raw(Env.class)));
	}

}
