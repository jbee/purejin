package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bind.BindingType;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.ValueBinder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.DefaultValueBinders;

import java.io.Serializable;
import java.util.RandomAccess;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.bind.ValueBinder.valueBinderTypeOf;

/**
 * An example of how to use {@link ValueBinder}s to customize the and binding
 * process.
 * <p>
 * This test demonstrates how the {@link DefaultValueBinders#REFERENCE_PREFER_CONSTANTS}
 * can be used to avoid bindings of type {@link BindingType#REFERENCE} where
 * possible.
 * <p>
 * This is the case when the referenced {@link Class} is banal in its
 * construction and it has no instance state on which it could depend. In such a
 * case the {@link DefaultValueBinders#REFERENCE_PREFER_CONSTANTS} will not create a
 * reference to the referenced type and a implicit bind to constructor but a
 * direct bind to a constant instance created during bootstrapping.
 * <p>
 * This might make sense in some situation but cause issues in others when there
 * cannot be multiple instances of the same type anyway for some reason.
 * <p>
 * For more examples on {@link ValueBinder}s have a look at:
 *
 * @see TestExampleCountBindingsBinds
 * @see TestExampleRequireConstructorParametersBinds
 * @see TestExampleFieldInjectionBinds
 */
class TestExamplePreferConstantsBinds {

	public static class BanalBean implements Serializable {
		// no state, default constructor
	}

	public static class NonBanalBean implements RandomAccess {

		public NonBanalBean(@SuppressWarnings("unused") Serializable state) {
		}
	}

	static class TestExamplePreferConstantsBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Serializable.class).to(BanalBean.class);
			bind(RandomAccess.class).to(NonBanalBean.class);
		}
	}

	@Test
	void referencesAreAvoidedWhenPossible() {
		Injector injector = injectorWithEnv(
				TestExamplePreferConstantsBindsModule.class,
				DefaultValueBinders.REFERENCE_PREFER_CONSTANTS);
		Serializable banal = injector.resolve(Serializable.class);
		assertSame(BanalBean.class, banal.getClass());
		RandomAccess nonBanal = injector.resolve(RandomAccess.class);
		assertSame(NonBanalBean.class, nonBanal.getClass());

		assertThrows(UnresolvableDependency.class,
				() -> injector.resolve(BanalBean.class),
				"Using a reference made the BanalBean available");
		assertNotNull(injector.resolve(NonBanalBean.class));
	}

	private static Injector injectorWithEnv(Class<? extends Bundle> root,
			ValueBinder<Instance<?>> binder) {
		return Bootstrap.injector(
				Bootstrap.DEFAULT_ENV.with(valueBinderTypeOf(Instance.class),
						binder), root);
	}
}
