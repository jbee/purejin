package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Resource;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test that demonstrates how {@link Injector.Observer} can be bound to track
 * the order of instance creation during the bootstrapping of an application
 * tree here simulated by types A, B and C.
 *
 * This sort of thing can be used to later tear down such instances in reverse
 * order.
 */
class TestBasicInjectorObserverBinds {

	public static class A {

		B b;

		public A(B b) {
			this.b = b;
		}

	}

	public static class B {

		C c;

		public B(C c) {
			this.c = c;
		}
	}

	public static class C {

	}

	public static class CreationObserver implements Injector.Observer {

		final List<Object> created = new ArrayList<>();

		@Override
		public void afterLift(Resource<?> resource, Object instance) {
			created.add(instance);
		}
	}

	private static class TestBasicInjectorObserverBindsModule extends BinderModule {

		@Override
		protected void declare() {
			construct(A.class);
			construct(B.class);
			construct(C.class);
			multibind(Injector.Observer.class).to(CreationObserver.class);
		}

	}

	private Injector context = Bootstrap.injector(
			TestBasicInjectorObserverBindsModule.class);

	@Test
	void orderOfCreationCanBeRecordedUsingInjectionListeners() {
		CreationObserver creation = context.resolve(CreationObserver.class);
		assertEquals(0, creation.created.size());
		A a = context.resolve(A.class);
		assertNotNull(a);
		assertEquals(3, creation.created.size());
		assertEquals(C.class, creation.created.get(0).getClass());
		assertEquals(B.class, creation.created.get(1).getClass());
		assertEquals(A.class, creation.created.get(2).getClass());
	}
}
