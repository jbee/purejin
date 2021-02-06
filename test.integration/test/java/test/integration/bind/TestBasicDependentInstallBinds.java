package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.binder.BundleFor;
import se.jbee.inject.bootstrap.Bootstrap;

import static se.jbee.junit.assertion.Assertions.assertEqualsIgnoreOrder;

/**
 * A test that shows how {@link BundleFor}s are used to install one or more (or
 * even none) of a set of options each implying the installation of a different
 * associated {@link se.jbee.inject.bind.Bundle}.
 * <p>
 * This can be used as a classic feature toggle that is configured in the {@link
 * Env} using the {@link Env#withDependent(Class, Enum[])} helper method so set
 * the selected options.
 * <p>
 * Secondly this technique of using {@link Enum}s constant to represent {@link
 * se.jbee.inject.bind.Bundle} classes allows to hide those implementations
 * behind the {@link Enum} that behaves as the API to outside world so it can
 * reference them when installing or uninstalling them.
 *
 * @see TestBasicBundleForBinds
 */
class TestBasicDependentInstallBinds {

	private enum Text {
		A, B, C, D, E
	}

	private static class A extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("A");
		}
	}

	private static class B extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("B");
		}
	}

	private static class C extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("C");
		}
	}

	private static class D extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("D");
		}
	}

	private static class E extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("E");
		}
	}

	private static class TextBundle extends BundleFor<Text> {

		@Override
		protected void bootstrap() {
			installDependentOn(Text.A, A.class);
			installDependentOn(Text.B, B.class);
			installDependentOn(Text.C, C.class);
			installDependentOn(Text.D, D.class);
			installDependentOn(Text.E, E.class);
		}
	}

	private static class RootBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(TextBundle.class, Text.class);
		}
	}

	@Test
	void multipleChoicesArePossible() {
		Env env = Bootstrap.DEFAULT_ENV.withDependent(Text.class, Text.A, Text.D);
		Injector injector = Bootstrap.injector(env, RootBundle.class);
		assertEqualsIgnoreOrder(new String[] { "A", "D" },
				injector.resolve(String[].class));
	}
}
