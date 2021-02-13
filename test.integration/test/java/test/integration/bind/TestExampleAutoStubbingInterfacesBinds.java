package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.lang.Type;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EventListener;

import static java.lang.reflect.Proxy.isProxyClass;
import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;
import static se.jbee.lang.Type.returnType;

/**
 * This test illustrates how to use binds with {@link Type#asUpperBound()} types
 * (wild-card types) to dynamically yield instances of different types that are
 * all sub-types of a common base class.
 * <p>
 * This can e.g. be used to "automatically" supply stubbing all dependencies
 * that have not be bound explicitly. A single declaration with a custom {@link
 * Supplier} is enough to achieve this.
 */
class TestExampleAutoStubbingInterfacesBinds {

	interface Shape {

		PathIterator getPathIterator(Object arg);

		Rectangle getBounds();

		Rectangle2D getBounds2D();
	}

	@FunctionalInterface
	interface PathIterator {

		boolean isDone();
	}

	interface MouseListener extends EventListener {

	}

	static class Rectangle {

		public Rectangle(int w, int h, int x, int y) {
		}

	}

	static class Rectangle2D {

	}

	private static class TestExampleAutoStubbingInterfacesBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			mockAnyUnbound(Object.class);
			mockAnyUnbound(EventListener.class);
			mockAnyUnbound(Shape.class);
			// bind some values returned from mock methods
			within(Mock.class).bind(anyOf(Rectangle.class)).to(
					new Rectangle(0, 0, 42, 42));
		}

		/**
		 * The unbound types mocks are limited to interfaces in this example
		 * implementation.
		 */
		private <B> void mockAnyUnbound(Class<? super B> base) {
			// type safety is essentially gone here as wild-card binds work the other way around:
			// they provide instances of any type that extends the base (not an instance that would be assignable from base)
			// so a wrongly implemented supplier would lead to a ClassCastExceptions
			@SuppressWarnings("unchecked")
			Type<B> upperBound = (Type<B>) raw(base).asUpperBound();
			per(Scope.injection).bind(anyOf(upperBound)).toSupplier(
					new MockSupplier<>(base));
		}

	}

	/**
	 * This interface is implemented by all mocks to illustrate how this could
	 * be used to build "rich" mocks that have a common interface to
	 * check/verify mock interaction.
	 */
	@FunctionalInterface
	interface Mock {

		/**
		 * Here the mocks just count invocations but this could be as
		 * sophisticated as needed to verify right behavior.
		 */
		int timesInvoked();
	}

	/**
	 * In this example mocks just work for interfaces as {@link Proxy} is used
	 * to create the mocks.
	 */
	static class MockSupplier<T> implements Supplier<T> {

		private final Class<?> base;

		public MockSupplier(Class<?> base) {
			this.base = base;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T supply(Dependency<? super T> dep, Injector context)
				throws UnresolvableDependency {
			Type<? super T> type = dep.type();
			if (!type.isInterface() || type.isUpperBound())
				throw new UnresolvableDependency.NoMethodForDependency(type);
			InvocationHandler handler = new MockInvocationHandler(base,
					context);
			ClassLoader cl = type.getClass().getClassLoader();
			Class<?>[] interfaces = new Class[] { Mock.class, type.rawType };
			return (T) Proxy.newProxyInstance(cl, interfaces, handler);
		}
	}

	static class MockInvocationHandler implements InvocationHandler {

		private final Class<?> base;
		private final Injector injector;
		private int timesInvoked = 0;

		public MockInvocationHandler(Class<?> base, Injector injector) {
			this.base = base;
			this.injector = injector;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			if (method.getDeclaringClass() == Mock.class) {
				return timesInvoked;
			}
			if ("toString".equals(method.getName())) {
				return base.getCanonicalName();
			}
			++timesInvoked;
			try {
				Name name = named(method.getDeclaringClass().getCanonicalName()
					+ "#" + method.getName());
				Dependency<?> dependency = dependency(
						instance(name, returnType(method)));
				// this hierarchy is used to allow bindings meant for mocks without risk of collisions
				dependency = dependency.injectingInto(Mock.class).injectingInto(
						method.getDeclaringClass()).ignoredScoping();
				return injector.resolve(dependency);
			} catch (UnresolvableDependency e) {
				Class<?> rt = method.getReturnType();
				// just to illustrate how to provide defaults for primitives
				if (rt.isPrimitive()) {
					if (int.class == rt)
						return 0;
					if (boolean.class == rt)
						return false;
					// and so forth...
				}
				return null;
			}
		}

	}

	private final Injector context = Bootstrap.injector(
			TestExampleAutoStubbingInterfacesBindsModule.class);

	@Test
	void wildcardBindsDoFallBackToMostGeneralIfRequired() {
		Serializable mock = context.resolve(Serializable.class);
		assertTrue(isProxyClass(mock.getClass()));
		assertEquals(Object.class.getCanonicalName(), mock.toString());
	}

	@Test
	void moreSpecificUpperBoundTypesAreMorePrecise() {
		MouseListener mock = context.resolve(MouseListener.class);
		assertTrue(isProxyClass(mock.getClass()));
		assertEquals(EventListener.class.getCanonicalName(), mock.toString());
	}

	@Test
	void bindsCanBeUsedToMockReturnValuesOfMockMethods() {
		Shape shape = context.resolve(Shape.class);
		Rectangle bounds = shape.getBounds();
		assertNotNull(bounds);
		assertTrue(shape instanceof Mock);
		Mock mock = (Mock) shape;
		assertEquals(1, mock.timesInvoked());
	}

	@Test
	void methodsOfMocksByDefaultReturnMocksIfPossible() {
		Shape shape = context.resolve(Shape.class);
		PathIterator iterator = shape.getPathIterator(null);
		assertNotNull(iterator);
		assertTrue(isProxyClass(iterator.getClass()));
		assertFalse(iterator.isDone());
	}

	@Test
	void methodOfMocksByDefaultReturnNullOtherwise() { // when no custom binding or mocking available
		Shape shape = context.resolve(Shape.class);
		assertNull(shape.getBounds2D());
	}
}
