package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Name.named;

import java.util.NoSuchElementException;

import org.junit.Test;

import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.extend.Config;

public class TestConfigBinds {

	private static final class TestConfigBindsModule extends BinderModule {

		@Override
		protected void declare() {
			config().bind(named("foo"), String.class).to("bar");
			config().bind(named("foo"), int.class).to(13);
			TargetedBinder testConfig = config(TestConfigBinds.class);
			testConfig.bind(named("foo"), String.class).to("que");
			testConfig.bind(named("foo"), int.class).to(42);
		}
	}

	private final Config config = Bootstrap.injector(
			TestConfigBindsModule.class).resolve(Config.class);

	@Test
	public void generalConfiguration() {
		assertEquals("bar", config.stringValue("foo"));
		assertEquals(13, config.intValue("foo"));
	}

	@Test
	public void namespacedConfiguration() {
		Config testConfig = config.of(TestConfigBinds.class);
		assertEquals("que", testConfig.stringValue("foo"));
		assertEquals(42, testConfig.intValue("foo"));
	}

	@Test(expected = NoSuchElementException.class)
	public void unknownValueThrowsException() {
		config.value("unknown", String.class).get();
	}

	public void unknownPrimitiveReturnsZero() {
		assertEquals(0, config.intValue("unknown"));
	}
}
