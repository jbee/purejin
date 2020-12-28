package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.ResourceResolutionFailed;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.jbee.inject.Name.named;

class TestBasicInjectorExceptionsBinds {

	private static class TestBasicInjectorExceptionsBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind(named("foo"), Integer.class).to(7);
			bind(named("bar"), Integer.class).to(8);
		}
	}

	private final Injector context = Bootstrap.injector(
			TestBasicInjectorExceptionsBindsModule.class);

	@Test
	void exceptionIsThrownWhenResolvingAnUnboundDependency() {
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(String.class));
	}

	@Test
	void exceptionIsThrownWhenResolvingAnUnboundDependencyWithBoundRawType() {
		assertThrows(ResourceResolutionFailed.class,
				() -> context.resolve(Name.DEFAULT, Integer.class));
	}

}
