package se.jbee.inject.bind;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.annotation.ElementType;

import javax.management.MXBean;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency.NoCaseForDependency;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Bootstrapper;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bundle;
import se.jbee.inject.bootstrap.ModuleWith;
import se.jbee.inject.config.Annotations;
import se.jbee.inject.config.Choices;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Options;

/**
 * Test that illustrates that the {@link Bootstrap} process adds bindings such
 * as binding {@link Globals}.
 */
public class TestBootstrapperModule {

	static class UninstallingBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			uninstall(Bootstrap.getBootstrapperBundle());
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
		Globals expected = Globals.STANDARD.with(
				Options.NONE.set(String.class, "custom"));
		Injector injector = Bootstrap.injector(EmptyBundle.class, expected);
		assertSame(expected, injector.resolve(Globals.class));
	}

	@Test
	public void optionsAreBoundByBootstrapper() {
		Options expected = Options.NONE.set(String.class, "custom");
		Globals globals = Globals.STANDARD.with(expected);
		Injector injector = Bootstrap.injector(EmptyBundle.class, globals);
		assertSame(expected, injector.resolve(Options.class));
	}

	@Test
	public void choicesAreBoundByBootstrapper() {
		Choices expected = Choices.NONE.choose(ElementType.TYPE);
		Globals globals = Globals.STANDARD.with(expected);
		Injector injector = Bootstrap.injector(EmptyBundle.class, globals);
		assertSame(expected, injector.resolve(Choices.class));
	}

	@Test
	public void annotationsAreBoundByBootstrapper() {
		Annotations expected = Annotations.DETECT.define(MXBean.class,
				new ModuleWith<Class<?>>() {

					@Override
					public void declare(Bindings bindings, Class<?> option) {
						// does not matter...
					}
				});
		Globals globals = Globals.STANDARD.with(expected);
		Injector injector = Bootstrap.injector(EmptyBundle.class, globals);
		assertSame(expected, injector.resolve(Annotations.class));
	}

	@Test
	public void standardBootstrapperModuleCanBeUninstalled() {
		Injector injector = Bootstrap.injector(UninstallingBundle.class);
		assertUnbound(Globals.class, injector);
		assertUnbound(Options.class, injector);
		assertUnbound(Choices.class, injector);
		assertUnbound(Annotations.class, injector);
	}

	private static void assertUnbound(Class<?> type, Injector injector) {
		try {
			injector.resolve(type);
			fail("Expected NoCaseForDependency");
		} catch (NoCaseForDependency e) {
			// expected
		}
	}
}
