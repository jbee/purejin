package se.jbee.inject.bootstrap;

import org.junit.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.bind.BindingType;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.bind.ValueBinder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.defaults.DefaultValueBinders;

import java.io.Serializable;
import java.util.RandomAccess;

import static org.junit.Assert.*;

/**
 * A test that demonstrates how the
 * {@link DefaultValueBinders#INSTANCE_REF_LITE} can be used to avoid bindings
 * of type {@link BindingType#REFERENCE} where possible. This is the case when
 * the referenced {@link Class} is banal in its construction and it has no
 * instance state on which it could depend. In such a case the
 * {@link DefaultValueBinders#INSTANCE_REF_LITE} will not create a reference to
 * the referenced type and a implicit bind to constructor but a direct bind to a
 * constant instance created during bootstrapping.
 *
 * This might make sense in some situation but cause issues in others when there
 * cannot be multiple instances of the same type anyway for some reason.
 */
public class TestCustomValueBinderBinds2 {

	static class BanalBean implements Serializable {
		// no state, default constructor
	}

	static class NonBanalBean implements RandomAccess {

		public NonBanalBean(@SuppressWarnings("unused") Serializable arg) {
		}

	}

	static class TestCustomValueBinderBinds2Module extends BinderModule {

		@Override
		protected void declare() {
			bind(Serializable.class).to(BanalBean.class);
			bind(RandomAccess.class).to(NonBanalBean.class);
		}
	}

	@Test
	public void configurationAllowsToAvoidedReferencesWhenPossible() {
		Injector injector = injectorWithEnv(
				TestCustomValueBinderBinds2Module.class,
				DefaultValueBinders.INSTANCE_REF_LITE);
		Serializable banal = injector.resolve(Serializable.class);
		assertSame(BanalBean.class, banal.getClass());
		RandomAccess nonBanal = injector.resolve(RandomAccess.class);
		assertSame(NonBanalBean.class, nonBanal.getClass());

		assertNotBound(BanalBean.class, injector);
		assertNotNull(injector.resolve(NonBanalBean.class));
	}

	private static void assertNotBound(Class<?> type, Injector context) {
		try {
			context.resolve(type);
			fail("Should not be bound");
		} catch (UnresolvableDependency e) {
			// expected
		}
	}

	private static Injector injectorWithEnv(Class<? extends Bundle> root,
			ValueBinder<?> binder) {
		return Bootstrap.injector(Bootstrap.ENV.withBinder(binder), root);
	}
}
