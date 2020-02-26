package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Bundle;

/**
 * A test that demonstrates how {@link Bundle}s (here in form of
 * {@link BinderModule}s) are installed for lazy bootstrapped {@link Injector}
 * sub-contexts.
 * 
 * This form of {@link Injector} nesting is not limited to any depth. In this
 * example the first level is the "foo" sub-context, the second level is the
 * "bar" sub-context defined in "foo".
 */
public class TestLazyInstallBinds {

	static final class TestLazyInstallBindsModule extends BinderModule {

		@Override
		protected void declare() {
			lazyInstall(LazyModule1.class, "foo");
			lazyInstall(LazyModule3.class, "foo");
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
			lazyInstall(LazyModule2.class, "bar");
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestLazyInstallBindsModule.class);

	@Test(expected = UnresolvableDependency.class)
	public void lazyInstallsAreNotAccessibleDirectly() {
		injector.resolve(int.class);
	}

	@Test
	public void lazyInstallsMakesSubInjectorAvailable() {
		Injector foo = injector.subContext("foo");
		assertNotSame(foo, injector);
		assertEquals(42, foo.resolve(int.class).intValue());
		assertEquals("13", foo.resolve(String.class));
		Injector bar = foo.subContext("bar");
		assertNotSame(bar, injector);
		assertNotSame(foo, bar);
		assertEquals("42", bar.resolve(String.class));
	}
}
