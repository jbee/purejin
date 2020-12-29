package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.Resource;
import se.jbee.inject.bind.Binding;
import se.jbee.inject.bind.Bindings;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.ValueBinder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.bind.ValueBinder.valueBinderTypeOf;

/**
 * An example of how to use {@link ValueBinder}s to customize the and binding
 * process.
 * <p>
 * The {@link CountBinder} illustrates that eventually all {@link Binding}s get
 * processed by a {@link ValueBinder} for type {@link Binding}s. If that {@link
 * ValueBinder} does not add these to the {@link Bindings} the resulting context
 * has no {@link Resource}s. But the {@link ValueBinder} can perform some check,
 * here symbolised by the act of simply counting the {@link Binding}. This sort
 * of thing would be most useful as a test of an application's configuration. It
 * allows to check it without actually bootstrapping it.
 * <p>
 * For more examples on {@link ValueBinder}s have a look at:
 *
 * @see TestExampleFieldInjectionBinds
 * @see TestExampleRequireConstructorParametersBinds
 * @see TestExamplePreferConstantsBinds
 */
class TestExampleCountBindingsBinds {

	private static class EmptyModule extends BinderModule {

		@Override
		protected void declare() {
			// acts as a reference as there are "build-in" bindings
			// that always exist
		}

	}

	private static final class CountBinder implements ValueBinder<Binding<?>> {

		int expands = 0;

		CountBinder() {
			// make visible
		}

		@Override
		public <T> void expand(Env env, Binding<?> ref, Binding<T> item,
				Bindings dest) {
			expands++;
		}
	}

	private static class TestExampleCountBindingsBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("answer");
			bind(Integer.class).to(42);
			bind(Boolean.class).to(true);
			bind(Number.class).to(Integer.class);
		}
	}

	@Test
	void bindingsCanJustBeCounted() {
		CountBinder count = new CountBinder();
		CountBinder emptyCount = new CountBinder();
		Injector injector = injectorWithEnv(
				TestExampleCountBindingsBindsModule.class, count);
		injectorWithEnv(EmptyModule.class, emptyCount);
		assertEquals(0, injector.resolve(Resource[].class).length);
		assertEquals(4, count.expands - emptyCount.expands);
	}

	private static Injector injectorWithEnv(Class<? extends Bundle> root,
			ValueBinder<Binding<?>> binder) {
		return Bootstrap.injector(Bootstrap.DEFAULT_ENV.with(
				valueBinderTypeOf(Binding.class), binder), root);
	}
}
