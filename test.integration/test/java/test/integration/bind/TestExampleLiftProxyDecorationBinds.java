package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Lift;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.lang.Type;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests demonstrates how to use {@link Lift}s to decorate the
 * {@link Injector} or bound instances.
 */
class TestExampleLiftProxyDecorationBinds {

	interface Shape {

		Object getBounds();
	}

	static final class Area implements Shape {

		@Override
		public Object getBounds() {
			return new Object();
		}
	}

	private static class TestExampleLiftProxyDecorationBindsModule
			extends BinderModule implements Lift<Shape> {

		@Override
		protected void declare() {
			lift().to((injector, as, __) -> new DelegatingInjector(injector));
			bind(int.class).to(42);
			bind(Foo.class).toConstructor();

			bind(Shape.class).toProvider(() -> new Area()); // OBS: A constant would not work as it is resolved using a shortcut
			lift(Shape.class).to(this);
		}

		@Override
		public Shape lift(Shape target, Type<?> as, Injector context) {
			return (Shape) Proxy.newProxyInstance(
					target.getClass().getClassLoader(),
					new Class[] { Shape.class },
					(proxy, method, args) -> method.invoke(target, args));
		}

	}

	public static class Foo {

		final Injector injector;

		public Foo(Injector injector) {
			this.injector = injector;
		}
	}

	private static class DelegatingInjector implements Injector {

		private final Injector delegate;

		DelegatingInjector(Injector delegate) {
			this.delegate = delegate;
		}

		@Override
		public <T> T resolve(Dependency<T> dependency)
				throws UnresolvableDependency {
			return delegate.resolve(dependency);
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestExampleLiftProxyDecorationBindsModule.class);

	@Test
	void injectorCanBeDecoratedUsingLifts() {
		assertSame(DelegatingInjector.class, injector.getClass());
	}

	@Test
	void decoratingInjectorCanResolveDependenciesUsingDelegate() {
		assertEquals(42, injector.resolve(int.class).intValue());
	}

	@Test
	void injectedInjectorCanBeDecoratedUsingLifts() {
		assertSame(DelegatingInjector.class,
				injector.resolve(Injector.class).getClass());
		assertSame(DelegatingInjector.class,
				injector.resolve(Foo.class).injector.getClass());
	}

	@Test
	void resolvedInstancesCanBeDecoratedUsingLifts() {
		Shape s = injector.resolve(Shape.class);
		assertNotNull(s);
		assertSame(s, injector.resolve(Shape.class));
		assertTrue(Proxy.isProxyClass(s.getClass()));
		assertNotNull(s.getBounds());
	}
}
