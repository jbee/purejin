package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Name.named;

import java.util.Properties;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Type;

/**
 * This test demonstrates how to use {@link Presets} to pass input data to the {@link Bootstrap}
 * that can be accessed in any {@link PresetModule} class. The value passed into
 * {@link PresetBinderModule#declare(Object)} is determined by the type of the generic. This has to
 * be the same {@link Type} as the one used when declaring the value via
 * {@link Presets#preset(Class, Object)}.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestPresetModuleBinds {

	private static class PresetModuleBindsModule
			extends PresetBinderModule<Properties> {

		@Override
		protected void declare( Properties preset ) {
			bind( named( "foo" ), String.class ).to( preset.getProperty( "foo.text" ) );
		}
	}

	private final Injector injector = Bootstrap.injector( PresetModuleBindsModule.class,
			Edition.FULL, Presets.NOTHING.preset( Properties.class, exampleProperties() ),
			Options.STANDARD );

	private Properties exampleProperties() {
		Properties props = new Properties();
		props.put( "foo.text", "bar" );
		return props;
	}

	@Test
	public void thatPresetValuePassedToModule() {
		assertEquals( "bar", injector.resolve( dependency( String.class ).named( "foo" ) ) );
	}
}
