package se.jbee.inject.bind;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.container.Typecast.listTypeOf;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.PresetModule;
import se.jbee.inject.config.Globals;
import se.jbee.inject.config.Presets;

/**
 * This test demonstrates how to use {@link Presets} to pass input data to the
 * {@link Bootstrap} that can be accessed in any {@link PresetModule} class. The
 * value passed into {@link BinderModuleWith#declare(Object)} is determined by
 * the type of the generic. This has to be the same {@link Type} as the one used
 * when declaring the value via {@link Presets#preset(Class, Object)}.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestPresetModuleBinds {

	private static class PresetModuleBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(PresetModuleBindsModule1.class);
			install(PresetModuleBindsModule2.class);
			install(PresetModuleBindsModule3.class);
		}

	}

	private static class PresetModuleBindsModule1
			extends BinderModuleWith<Properties> {

		@Override
		protected void declare(Properties preset) {
			bind(named("foo"), String.class).to(preset.getProperty("foo.text"));
		}
	}

	private static class PresetModuleBindsModule2
			extends BinderModuleWith<List<String>> {

		@Override
		protected void declare(List<String> preset) {
			bind(named("list"), String.class).to(preset.get(1));
		}

	}

	private static class PresetModuleBindsModule3
			extends BinderModuleWith<List<Integer>> {

		@Override
		protected void declare(List<Integer> preset) {
			bind(named("list"), Integer.class).to(preset.get(1));
		}

	}

	private static class PresetModuleBindsModule4
			extends BinderModuleWith<Presets> {

		@Override
		protected void declare(Presets preset) {
			assertNotNull(preset);
		}

	}

	private final Injector injector = injector();

	private static Injector injector() {
		Presets presets = Presets.EMPTY.preset(Properties.class,
				exampleProperties()).preset(listTypeOf(String.class),
						asList("a", "b")).preset(listTypeOf(Integer.class),
								asList(1, 2));
		return Bootstrap.injector(PresetModuleBindsBundle.class,
				Globals.STANDARD.presets(presets));
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
	public void presetItselfCanBePassedToModule() {
		assertNotNull(Bootstrap.injector(PresetModuleBindsModule4.class));
	}
}
