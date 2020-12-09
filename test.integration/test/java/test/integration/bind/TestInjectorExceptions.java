package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.jbee.inject.Name.named;

class TestInjectorExceptions {

	private static class TestInjectorBundle extends BinderModule {

		@Override
		protected void declare() {
			bind(named("foo"), Integer.class).to(7);
			bind(named("bar"), Integer.class).to(8);
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestInjectorBundle.class);

	@Test
	void thatExceptionIsThrownWhenResolvingAnUnboundDependency() {
		assertThrows(NoResourceForDependency.class, () -> injector.resolve(String.class));
	}

	@Test
	void thatExceptionIsThrownWhenResolvingAnUnboundDependencyWithBoundRawType() {
		assertThrows(NoResourceForDependency.class, () -> injector.resolve(Name.DEFAULT, Integer.class));
	}

}
