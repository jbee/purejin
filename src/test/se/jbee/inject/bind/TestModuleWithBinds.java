package se.jbee.inject.bind;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.container.Cast.listTypeOf;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.config.Env;
import se.jbee.inject.config.Options;
import se.jbee.inject.declare.ModuleWith;

/**
 * This test demonstrates how to use {@link Options} to pass input data to the
 * {@link Bootstrap} that can be accessed in any {@link ModuleWith} class. The
 * value passed into {@link BinderModuleWith#declare(Object)} is determined by
 * the type of the generic. This has to be the same {@link Type} as the one used
 * when declaring the value via {@link Options#set(Class, Object)}.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestModuleWithBinds {

	private static class TestModuleWithBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(TestModuleWithBindsModule1.class);
			install(TestModuleWithBindsModule2.class);
			install(TestModuleWithBindsModule3.class);
		}

	}

	private static class TestModuleWithBindsModule1
			extends BinderModuleWith<Properties> {

		@Override
		protected void declare(Properties preset) {
			bind(named("foo"), String.class).to(preset.getProperty("foo.text"));
		}
	}

	private static class TestModuleWithBindsModule2
			extends BinderModuleWith<List<String>> {

		@Override
		protected void declare(List<String> preset) {
			bind(named("list"), String.class).to(preset.get(1));
		}

	}

	private static class TestModuleWithBindsModule3
			extends BinderModuleWith<List<Integer>> {

		@Override
		protected void declare(List<Integer> preset) {
			bind(named("list"), Integer.class).to(preset.get(1));
		}

	}

	private static class TestModuleWithBindsModule4
			extends BinderModuleWith<Env> {

		@Override
		protected void declare(Env preset) {
			assertNotNull(preset);
		}

	}

	private final Injector injector = injector();

	private static Injector injector() {
		Env env = Bootstrap.ENV //
				.with(Properties.class, exampleProperties()) //
				.with(listTypeOf(String.class), asList("a", "b")) //
				.with(listTypeOf(Integer.class), asList(1, 2));
		return Bootstrap.injector(TestModuleWithBindsBundle.class, env);
	}

	private static Properties exampleProperties() {
		Properties props = new Properties();
		props.put("foo.text", "bar");
		return props;
	}

	@Test
	public void thatPresetValuePassedToModule() {
		assertEquals("bar", injector.resolve("foo", String.class));
	}

	@Test
	public void thatDifferentParametizedPresetValuesForSameGenericTypeArePosssible() {
		assertEquals("b", injector.resolve("list", String.class));
		assertEquals(2, injector.resolve("list", Integer.class).intValue());
	}

	@Test
	public void envItselfCanBePassedToModule() {
		assertNotNull(Bootstrap.injector(TestModuleWithBindsModule4.class));
	}
}
