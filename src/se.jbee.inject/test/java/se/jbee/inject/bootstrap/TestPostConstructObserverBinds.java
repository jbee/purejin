package se.jbee.inject.bootstrap;

import org.junit.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Resource;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.container.PostConstructObserver;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test that demonstrates how {@link PostConstructObserver} can be bound to track
 * the order of instance creation during the bootstrapping of an application
 * tree here simulated by types A, B and C.
 *
 * This sort of thing can be used to later tear down such instances in reverse
 * order.
 */
public class TestPostConstructObserverBinds {

	static class A {

		B b;

		A(B b) {
			this.b = b;
		}

	}

	static class B {

		C c;

		B(C c) {
			this.c = c;
		}
	}

	static class C {

	}

	static class CreationObserver implements PostConstructObserver {

		final List<Object> created = new ArrayList<>();

		@Override
		public <T> void afterPostConstruct(Resource<T> resource, T instance) {
			created.add(instance);
		}
	}

	static class TestPostConstructObserverBindsModule extends BinderModule {

		@Override
		protected void declare() {
			construct(A.class);
			construct(B.class);
			construct(C.class);
			multibind(PostConstructObserver.class).to(CreationObserver.class);
		}

	}

	private Injector injector = Bootstrap.injector(
			TestPostConstructObserverBindsModule.class);

	@Test
	public void orderOfCreationCanBeRecordedUsingInjectionListeners() {
		CreationObserver creation = injector.resolve(CreationObserver.class);
		assertEquals(0, creation.created.size());
		A a = injector.resolve(A.class);
		assertNotNull(a);
		assertEquals(3, creation.created.size());
		assertEquals(C.class, creation.created.get(0).getClass());
		assertEquals(B.class, creation.created.get(1).getClass());
		assertEquals(A.class, creation.created.get(2).getClass());
	}
}
