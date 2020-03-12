package se.jbee.inject.bind;

import static org.junit.Assert.fail;

import java.lang.annotation.ElementType;

import javax.management.MXBean;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency.NoResourceForDependency;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Bootstrapper;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.config.Env;
import se.jbee.inject.config.Globals;
import se.jbee.inject.declare.Bundle;
import se.jbee.inject.declare.ModuleWith;

/**
 * Test that illustrates that the {@link Bootstrap} process adds bindings such
 * as binding {@link Globals}.
 */
public class TestBootstrapperModule {

	static class UninstallingBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			//TODO uninstall ...
		}
	}

	static class EmptyBundle implements Bundle {

		@Override
		public void bootstrap(Bootstrapper bootstrap) {
			// do nothing
		}
	}

	@Test
	public void globalsAreBoundByBootstrapper() {
		Env env = Bootstrap.ENV.with(String.class, "custom");
		Injector injector = Bootstrap.injector(EmptyBundle.class, env);
		//TODO env test
	}

	@Test
	public void optionsAreBoundByBootstrapper() {
		Env env = Bootstrap.ENV.with(String.class, "custom");
		Injector injector = Bootstrap.injector(EmptyBundle.class, env);
		//TODO env test
	}

	@Test
	public void choicesAreBoundByBootstrapper() {
		Env env = Bootstrap.ENV.with(ElementType.class,
				ElementType.TYPE);
		Injector injector = Bootstrap.injector(EmptyBundle.class, env);
		//TODO env test
	}

	@Test
	public void annotationsAreBoundByBootstrapper() {
		ModuleWith<Class<?>> effect = new ModuleWith<Class<?>>() {

			@Override
			public void declare(Bindings bindings, Env env,
					Class<?> option) {
				// does not matter...
			}
		};
		Env env = Bootstrap.ENV.withAnnotation(MXBean.class, effect);
		Injector injector = Bootstrap.injector(EmptyBundle.class, env);
		//TODO env test
	}

	@Test
	public void standardBootstrapperModuleCanBeUninstalled() {
		Injector injector = Bootstrap.injector(UninstallingBundle.class);
		//TODO new test that defaults are gone test
	}

	private static void assertUnbound(Class<?> type, Injector injector) {
		try {
			injector.resolve(type);
			fail("Expected " + NoResourceForDependency.class.getName());
		} catch (NoResourceForDependency e) {
			// expected
		}
	}
}
