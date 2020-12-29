package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.DeclarationType;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Binding;
import se.jbee.inject.bind.Module;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.jbee.inject.DeclarationType.EXPLICIT;

/**
 * Making sure the general functions of the {@link BinderModule} work as
 * expected.
 */
class TestBasicMultiModuleSetupBinds {

	static class TestBasicMultiModuleSetupBindsModule1 extends BinderModule {

		@Override
		protected void declare() {
			bind(Integer.class).to(1);
			bind(String.class).to("2");
			bind(Float.class).to(3.0f);
		}
	}

	static class TestBasicMultiModuleSetupBindsModule2 extends BinderModule {

		@Override
		protected void declare() {
			bind(Double.class).to(2.0);
		}
	}

	static class TestBasicMultiModuleSetupBindsBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(TestBasicMultiModuleSetupBindsModule1.class);
			install(TestBasicMultiModuleSetupBindsModule2.class);
		}
	}

	@Test
	void bindingSourceReflectsTheOrigin() {
		Injector context = Bootstrap.injector(Bootstrap.DEFAULT_ENV //
						.with(Env.BIND_BINDINGS, true),
				TestBasicMultiModuleSetupBindsBundle.class);
		Binding<?>[] bindings = context.resolve(Binding[].class);

		assertBinding(TestBasicMultiModuleSetupBindsModule1.class, 1, EXPLICIT,
				forType(Integer.class, bindings));
		assertBinding(TestBasicMultiModuleSetupBindsModule1.class, 2, EXPLICIT,
				forType(String.class, bindings));
		assertBinding(TestBasicMultiModuleSetupBindsModule1.class, 3, EXPLICIT,
				forType(Float.class, bindings));
		assertBinding(TestBasicMultiModuleSetupBindsModule2.class, 1, EXPLICIT,
				forType(Double.class, bindings));
	}

	static void assertBinding(Class<? extends Module> module, int no,
			DeclarationType type, Binding<?> binding) {
		assertNotNull(binding);
		assertEquals(module, binding.source.ident);
		assertEquals(no, binding.source.declarationNo);
		assertEquals(type, binding.source.declarationType);
	}

	@SuppressWarnings("unchecked")
	private static <T> Binding<T> forType(Class<T> type,
			Binding<?>[] bindings) {
		for (Binding<?> b : bindings) {
			if (b.type().rawType == type)
				return (Binding<T>) b;
		}
		return null;
	}
}
