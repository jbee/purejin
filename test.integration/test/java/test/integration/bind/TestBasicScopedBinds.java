package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency.UnstableDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Very simple demo of the {@link se.jbee.inject.ScopeLifeCycle}s that help to
 * detect scoping errors, such as trying to inject an instance in {@link
 * Scope#injection} into another one in {@link Scope#application}.
 */
class TestBasicScopedBinds {

	private static class Foo {

		@SuppressWarnings("unused")
		Foo(Bar bar) {
			// it is just about the instances
		}
	}

	private static class Bar {
		// just to demo
	}

	private static class TestBasicScopedBindsModule extends BinderModule {

		@Override
		protected void declare() {
			per(Scope.application).construct(Foo.class);
			per(Scope.injection).construct(Bar.class);
		}
	}

	private final Injector context = Bootstrap.injector(
			TestBasicScopedBindsModule.class);

	@Test
	void injectingAnInjectionScopedInstanceIntoAppScopedInstanceThrowsAnException() {
		Exception ex = assertThrows(UnstableDependency.class, () -> context.resolve(Foo.class));
		assertEquals("Unstable dependency injection\n"
				+ "\t  of: test.integration.bind.TestBasicScopedBinds.Bar scoped injection\n"
				+ "\tinto: test.integration.bind.TestBasicScopedBinds.Foo scoped application",
				ex.getMessage());
	}
}
