package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Module;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.Binder.PluginBinder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.Plugins;

import java.io.Serializable;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.junit.assertion.Assertions.assertEqualsIgnoreOrder;

/**
 * Plug-in-binds are a convenient way to define a named set of classes. The name
 * of the set is given by the class stated in the {@link
 * PluginBinder#into(Class)} method. The {@link Binder#plug(Class)} states the
 * class that should be added to a set.
 * <p>
 * The set than can be received from the {@link Injector} using {@link
 * Plugins#forPoint(Class)}. This allows to easily build abstractions that use
 * this to collect the set of classes their effect applies to.
 * <p>
 * As the name suggest this is a perfect way to build a plugin system.
 * <p>
 * Note that the sets are just ordinary bindings of type {@link Class}. That
 * means the sets can contain different members depending on the actual {@link
 * Dependency} if the scope of the bind is narrowed (localised) like it is done
 * in this example.
 */
class TestBasicPluginBinds {

	private static class TestBasicPluginBindsModule extends BinderModule {

		@Override
		protected void declare() {
			// testing effectiveness of usual targeting
			asDefault().plug(ExtensionAction.class).into(Callable.class);
			inPackageOf(Module.class).plug(
					ExtensionPackageLocalAction.class).into(Callable.class);
			injectingInto(Serializable.class).plug(
					ExtensionInstanceOfAction.class).into(Callable.class);

			// testing elimination of duplicates
			plug(Integer.class).into(Long.class);
			plug(Integer.class).into(Long.class); // 2nd time
			plug(Float.class).into(Long.class);

			// testing ContextAware
			construct(ContextAwarePlugins.class);
			// just so we have a value as well
			injectingInto(ContextAwarePlugins.class)
					.plug(String.class)
					.into(CharSequence.class);
		}
	}

	private static class ExtensionAction {
		// just to see that it is resolved as action class
	}

	private static class ExtensionPackageLocalAction {
		// just to see that it is resolved as action class
	}

	private static class ExtensionInstanceOfAction {
		// just to see that it is resolved as action class
	}

	public static class ContextAwarePlugins {

		final Plugins plugins;

		public ContextAwarePlugins(Plugins plugins) {
			this.plugins = plugins;
		}
	}

	private final Injector context = Bootstrap.injector(
			TestBasicPluginBindsModule.class);

	private final Plugins plugins = context.resolve(Plugins.class);

	@Test
	void onlyNotTargetedExtensionsAreResolvedGlobally() {
		Class<?>[] classes = plugins.forPoint(Callable.class);
		assertEquals(1, classes.length);
		assertSame(ExtensionAction.class, classes[0]);
	}

	@Test
	void packageLocalExtensionsAreResolvedWithAppropriateInjection() {
		Class<?>[] classes = plugins.targeting(Module.class).forPoint(
				Callable.class);
		assertEqualsIgnoreOrder(new Class<?>[] { ExtensionAction.class,
				ExtensionPackageLocalAction.class }, classes);
	}

	@Test
	void instanceOfExtensionsAreResolvedWithAppropriateInjection() {
		Class<?>[] classes = plugins.targeting(String.class).forPoint(
				Callable.class);
		assertEqualsIgnoreOrder(new Class<?>[] { ExtensionAction.class,
				ExtensionInstanceOfAction.class }, classes);
	}

	@Test
	void duplicatesAreEliminated() {
		Class<?>[] classes = plugins.forPoint(Long.class);
		assertEqualsIgnoreOrder(new Class<?>[] { Integer.class, Float.class }, classes);
	}

	@Test
	void pluginsAreInjectedContextAware() {
		ContextAwarePlugins bean = context.resolve(ContextAwarePlugins.class);
		assertNotNull(bean);
		assertNotNull(bean.plugins);
		assertEquals(ContextAwarePlugins.class, bean.plugins.getTarget());
		assertEqualsIgnoreOrder(new Class<?>[] { String.class },
				bean.plugins.forPoint(CharSequence.class));
	}
}
