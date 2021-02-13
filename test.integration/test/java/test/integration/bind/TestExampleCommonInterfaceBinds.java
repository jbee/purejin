package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;
import static se.jbee.junit.assertion.Assertions.assertEqualsIgnoreOrder;

/**
 * Solution for cycle on common interface injecting other implementations into
 * one of them.
 */
class TestExampleCommonInterfaceBinds {

	interface A {

	}

	public static class B implements A {

		final A[] as;

		public B(A[] as) {
			this.as = as;
		}
	}

	public static class C implements A {

	}

	public static class D implements A {

	}

	static class Module1 extends BinderModule {

		@Override
		protected void declare() {
			bind("left", A.class).to(B.class);
			bind("right", A.class).to("left", B.class);
			bind("left", B.class).toConstructor();
			bind(B.class).toConstructor();
			injectingInto("left", B.class).bind(A[].class).to("special", A[].class);
			arraybind(A[].class).to(new A[0]);
		}
	}

	static class Module2 extends BinderModule {

		@Override
		protected void declare() {
			multibind("special", A.class).to(C.class);
		}
	}

	static class Module3 extends BinderModule {

		@Override
		protected void declare() {
			multibind("special", A.class).to(D.class);
		}

	}

	static class TestExampleCommonInterfaceBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(Module1.class);
			install(Module2.class);
			install(Module3.class);
		}

	}

	@Test
	void thatBundleCanBeBootstrapped() {
		Injector injector = Bootstrap.injector(
				TestExampleCommonInterfaceBindsBundle.class);
		B b = injector.resolve(B.class);
		B leftB = injector.resolve("left", B.class);
		assertNotSame(b, leftB);
		assertEquals(2, leftB.as.length);
		C c = injector.resolve(dependency(C.class).injectingInto(
				instance(named("left"), raw(B.class))));
		D d = injector.resolve(dependency(D.class).injectingInto(
				instance(named("left"), raw(B.class))));
		assertEqualsIgnoreOrder(new A[] { c, d }, leftB.as);
	}
}
