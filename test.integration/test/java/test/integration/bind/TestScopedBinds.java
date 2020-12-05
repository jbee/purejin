package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency.UnstableDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TestScopedBinds {

	private static class Foo {

		@SuppressWarnings("unused")
		Foo(Bar bar) {
			// it is just about the instances
		}
	}

	private static class Bar {
		// just to demo
	}

	private static class ScopedBindsModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.application).construct(Foo.class);
			per(Scope.injection).construct(Bar.class);
		}
	}

	@Test
	void thatInjectingAnInjectionScopedInstanceIntoAppScopedInstanceThrowsAnException() {
		Injector injector = Bootstrap.injector(ScopedBindsModule.class);
		assertThrows(UnstableDependency.class, () -> injector.resolve(Foo.class));
	}

}
