package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.lang.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.lang.Type.raw;

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

	private final Injector context = Bootstrap.injector(
			ConstantBindsModule.class);

	@Test
	void instanceInjectedBasedOnTheDependencyType() {
		assertInjects("bar", raw(CharSequence.class));
		assertInjects("foobar", raw(String.class));
		assertInjects(42, raw(Integer.class));
	}

	private <T> void assertInjects(T expected, Type<? extends T> forActual) {
		assertEquals(expected, context.resolve(forActual));
	}
}
