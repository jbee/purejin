package se.jbee.inject.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.Shape;
import java.awt.geom.Area;
import java.lang.reflect.Proxy;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Initialiser;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * Tests demonstrates how to use {@link Initialiser}s to decorate the
 * {@link Injector} or bound instances.
 */
public class TestInitialiserDecorationBinds {

	private static class TestInitialiserDecorationModule extends BinderModule
			implements Initialiser<Shape> {

		@Override
		protected void declare() {
			initbind().to((injector, __) -> new DelegatingInjector(injector));
			bind(int.class).to(42);
			bind(Foo.class).toConstructor();

			bind(Shape.class).to(() -> new Area()); // OBS: A constant would not work as it is resolved using a shortcut
			initbind(Shape.class).to(this);
		}

		@Override
		public Shape init(Shape target, Injector context) {
			return (Shape) Proxy.newProxyInstance(
					target.getClass().getClassLoader(),
					new Class[] { Shape.class },
					(proxy, method, args) -> method.invoke(target, args));
		}

	}

	static class Foo {

		final Injector injector;

		Foo(Injector injector) {
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
			TestInitialiserDecorationModule.class);

	@Test
	public void injectorCanBeDecoratedUsingInitialisers() {
		assertSame(DelegatingInjector.class, injector.getClass());
	}

	@Test
	public void decoratingInjectorCanResolveDependenciesUsingDelegate() {
		assertEquals(42, injector.resolve(int.class).intValue());
	}

	@Test
	public void injectedInjectorCanBeDecoratedUsingInitialisers() {
		assertSame(DelegatingInjector.class,
				injector.resolve(Injector.class).getClass());
		assertSame(DelegatingInjector.class,
				injector.resolve(Foo.class).injector.getClass());
	}

	@Test
	public void resolvedInstancesCanBeDocratedUsingInitialisers() {
		Shape s = injector.resolve(Shape.class);
		assertNotNull(s);
		assertSame(s, injector.resolve(Shape.class));
		assertTrue(Proxy.isProxyClass(s.getClass()));
		assertNotNull(s.getBounds());
	}
}
