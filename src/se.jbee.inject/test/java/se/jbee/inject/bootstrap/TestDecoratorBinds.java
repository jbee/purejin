package se.jbee.inject.bootstrap;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * Test the fix for issue #61, falsely detection of dependency cycle when using
 * the decorator pattern.
 */
public class TestDecoratorBinds {

	interface Foo {
		// the used abstraction
	}

	static class Decorator implements Foo {

		final Foo decorated;

		public Decorator(Foo decorated) {
			this.decorated = decorated;
		}

	}

	static class Bar implements Foo {
		// a special Foo for the Decorator
	}

	static class DecoratorBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Foo.class).to(Decorator.class);
			injectingInto(Decorator.class).bind(Foo.class).to(Bar.class);
		}

	}

	@Test
	public void test() {
		Injector injector = Bootstrap.injector(DecoratorBindsModule.class);
		assertEquals(Decorator.class, injector.resolve(Foo.class).getClass());
	}
}
