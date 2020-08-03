package se.jbee.inject.bootstrap;

import org.junit.Test;
import se.jbee.inject.Converter;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.config.Config;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static se.jbee.inject.Name.named;

public class TestConfigBinds {

	static final class Bean {

		final Config config;

		public Bean(Config config) {
			this.config = config;
		}
	}

	private static final class TestConfigBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Bean.class).toConstructor();
			config().bind(named("foo"), String.class).to("bar");
			config().bind(named("foo"), int.class).to(13);
			TargetedBinder testConfig = config(Bean.class);
			testConfig.bind(named("foo"), String.class).to("que");
			testConfig.bind(named("foo"), int.class).to(42);
			bind(Converter.type(String.class, UUID.class)).to(UUID::fromString);
			config().bind(named("uuid"), String.class).to(
					UUID.randomUUID().toString());
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestConfigBindsModule.class);
	private final Config config = injector.resolve(Config.class);

	@Test
	public void generalConfiguration() {
		assertEquals("bar", config.stringValue("foo"));
		assertEquals(13, config.intValue("foo"));
	}

	@Test
	public void namespacedConfiguration() {
		Config beanConfig = config.of(Bean.class);
		assertEquals("que", beanConfig.stringValue("foo"));
		assertEquals(42, beanConfig.intValue("foo"));
	}

	@Test
	public void namespacedConfigurationIsInjected() {
		Config beanConfig = injector.resolve(Bean.class).config;
		assertEquals("que", beanConfig.stringValue("foo"));
		assertEquals(42, beanConfig.intValue("foo"));
	}

	@Test(expected = NoSuchElementException.class)
	public void unknownValueThrowsException() {
		config.optionalValue(String.class, "unknown").get();
	}

	@Test
	public void unknownPrimitiveReturnsZero() {
		assertEquals(0, config.intValue("unknown"));
	}

	@Test
	public void convertedConfiguration() {
		assertNotNull(config.value("uuid").as(UUID.class).get());
	}
}
