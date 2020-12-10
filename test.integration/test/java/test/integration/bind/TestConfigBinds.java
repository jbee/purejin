package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Converter;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.Config;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Name.named;

class TestConfigBinds {

	public static final class Bean {

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
			bind(Converter.converterTypeOf(String.class, UUID.class)).to(UUID::fromString);
			config().bind(named("uuid"), String.class).to(
					UUID.randomUUID().toString());
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestConfigBindsModule.class);
	private final Config config = injector.resolve(Config.class);

	@Test
	void generalConfiguration() {
		assertEquals("bar", config.stringValue("foo"));
		assertEquals(13, config.intValue("foo"));
	}

	@Test
	void namespacedConfiguration() {
		Config beanConfig = config.of(Bean.class);
		assertEquals("que", beanConfig.stringValue("foo"));
		assertEquals(42, beanConfig.intValue("foo"));
	}

	@Test
	void namespacedConfigurationIsInjected() {
		Config beanConfig = injector.resolve(Bean.class).config;
		assertEquals("que", beanConfig.stringValue("foo"));
		assertEquals(42, beanConfig.intValue("foo"));
	}

	@Test
	void unknownValueThrowsException() {
		Optional<String> unknown = config.optionalValue(String.class,
				"unknown");
		assertThrows(NoSuchElementException.class, unknown::get);
	}

	@Test
	void unknownPrimitiveReturnsZero() {
		assertEquals(0, config.intValue("unknown"));
	}

	@Test
	void convertedConfiguration() {
		assertNotNull(config.value("uuid").as(UUID.class).orElse(null));
	}
}
