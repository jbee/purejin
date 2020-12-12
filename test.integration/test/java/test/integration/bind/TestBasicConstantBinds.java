package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.CoreFeature;
import se.jbee.inject.lang.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.lang.Type.raw;

class TestBasicConstantBinds {

	private static class ConstantBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("foobar");
			bind(CharSequence.class).to("bar");
			bind(Integer.class).to(42);
			bind(Float.class).to(42.0f);
		}

	}

	private static class InstanceBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			installAll(CoreFeature.class);
			install(ConstantBindsModule.class);
		}

	}

	private final Injector injector = Bootstrap.injector(
			InstanceBindsBundle.class);

	@Test
	void thatInstanceInjectedBasedOnTheDependencyType() {
		assertInjects("bar", raw(CharSequence.class));
		assertInjects("foobar", raw(String.class));
		assertInjects(42, raw(Integer.class));
	}

	private <T> void assertInjects(T expected,
			Type<? extends T> dependencyType) {
		assertEquals(expected, injector.resolve(dependencyType));
	}
}
