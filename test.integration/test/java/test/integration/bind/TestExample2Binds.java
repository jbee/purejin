package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static se.jbee.inject.Name.named;

/**
 * This illustrates how to use different named instances for the same interface
 * that are all implemented by the same class without having them linked to the
 * same instance.
 */
class TestExample2Binds {

	interface Decoupling {

	}

	public static class DefaultImpl implements Decoupling {

	}

	static class Example2Module extends BinderModule {

		@Override
		protected void declare() {
			bind(named("a"), Decoupling.class).toConstructor(DefaultImpl.class);
			bind(named("b"), Decoupling.class).toConstructor(DefaultImpl.class);
			bind(named("c"), Decoupling.class).toConstructor(DefaultImpl.class);
			bind(named("d"), Decoupling.class).toConstructor(DefaultImpl.class);
		}

	}

	@Test
	void thatInstancesAToDAreDifferent() {
		Injector injector = Bootstrap.injector(Example2Module.class);
		Decoupling a = injector.resolve("a", Decoupling.class);
		Decoupling b = injector.resolve("b", Decoupling.class);
		Decoupling c = injector.resolve("c", Decoupling.class);
		Decoupling d = injector.resolve("d", Decoupling.class);

		assertNotSame(a, b);
		assertNotSame(a, c);
		assertNotSame(a, d);
		assertNotSame(b, c);
		assertNotSame(b, d);
		assertNotSame(c, d);
	}
}
