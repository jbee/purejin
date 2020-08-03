package se.jbee.inject.bootstrap;

import static se.jbee.inject.Name.named;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

public class TestInjectorExceptions {

	private static class TestInjectorBundle extends BinderModule {

		@Override
		protected void declare() {
			bind(named("foo"), Integer.class).to(7);
			bind(named("bar"), Integer.class).to(8);
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestInjectorBundle.class);

	@Test(expected = NoResourceForDependency.class)
	public void thatExceptionIsThrownWhenResolvingAnUnboundDependency() {
		injector.resolve(String.class);
	}

	@Test(expected = NoResourceForDependency.class)
	public void thatExceptionIsThrownWhenResolvingAnUnboundDependencyWithBoundRawType() {
		injector.resolve(Name.DEFAULT, Integer.class);
	}

}
