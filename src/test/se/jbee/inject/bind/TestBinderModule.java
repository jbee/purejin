package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.DeclarationType.EXPLICIT;

import org.junit.Test;

import se.jbee.inject.DeclarationType;
import se.jbee.inject.bootstrap.Binding;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.Module;
import se.jbee.inject.config.Globals;

/**
 * Making sure the general functions of the {@link BinderModule} work as
 * expected.
 */
public class TestBinderModule {

	static class TestBinderModuleModule1 extends BinderModule {

		@Override
		protected void declare() {
			bind(Integer.class).to(1);
			bind(String.class).to("2");
			bind(Float.class).to(3.0f);
		}

	}

	static class TestBinderModuleModule2 extends BinderModule {

		@Override
		protected void declare() {
			bind(Double.class).to(2.0);
		}

	}

	static class TestBinderModuleBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(TestBinderModuleModule1.class);
			install(TestBinderModuleModule2.class);
		}

	}

	@Test
	public void thatBindingSourceReflectsTheOrigin() {
		Binding<?>[] bindings = Bootstrap.bindings(TestBinderModuleBundle.class,
				Bindings.newBindings(), Globals.STANDARD);

		assertEquals(4 + 10, bindings.length); // 10 scopes

		assertBinding(TestBinderModuleModule1.class, 1, EXPLICIT,
				forType(Integer.class, bindings));
		assertBinding(TestBinderModuleModule1.class, 2, EXPLICIT,
				forType(String.class, bindings));
		assertBinding(TestBinderModuleModule1.class, 3, EXPLICIT,
				forType(Float.class, bindings));
		assertBinding(TestBinderModuleModule2.class, 1, EXPLICIT,
				forType(Double.class, bindings));
	}

	static void assertBinding(Class<? extends Module> module, int no,
			DeclarationType type, Binding<?> binding) {
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
