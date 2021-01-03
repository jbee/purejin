package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Source;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.Config;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.Converter.converterTypeOf;
import static se.jbee.inject.Name.named;

/**
 * The {@link Config} {@link se.jbee.inject.config.Extension} is a helper to
 * organise runtime configuration of the application that is made as part of the
 * binding process.
 * <p>
 * Localised binds are used to declare the individual configuration properties.
 * To ease this process the {@link se.jbee.inject.binder.Binder.ScopedBinder#configure(Class)}
 * helps to understand localisation as configuration for a specific target
 * {@link Class} or {@link se.jbee.inject.Instance}.
 * <p>
 * This configuration is then accessed by injecting the {@link Config} into the
 * target type.
 */
class TestBasicConfigurationBinds {

	public static final class Bean {

		final Config config;

		public Bean(Config config) {
			this.config = config;
		}
	}

	private static final class TestBasicConfigurationBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind(Bean.class).toConstructor();
			configure().bind(named("foo"), String.class).to("bar");
			configure().bind(named("foo"), int.class).to(13);
			TargetedBinder beanConfig = configure(Bean.class);
			beanConfig.bind(named("foo"), String.class).to("que");
			beanConfig.bind(named("foo"), int.class).to(42);
			bind(converterTypeOf(String.class, UUID.class)).to(
					UUID::fromString);
			configure().bind(named("uuid"), String.class).to(
					UUID.randomUUID().toString());
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestBasicConfigurationBindsModule.class);
	private final Config config = injector.resolve(Config.class);

	@Test
	void generalConfiguration() {
		assertEquals("bar", config.stringValue("foo"));
		assertEquals(13, config.intValue("foo"));
	}

	@Test
	void perTypeConfiguration() {
		Config beanConfig = config.of(Bean.class);
		assertEquals("que", beanConfig.stringValue("foo"));
		assertEquals(42, beanConfig.intValue("foo"));
	}

	@Test
	void configurationIsInjectedInTargetNamespace() {
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

	@Test
	void sourceOfConfigValueIsAvailable() {
		Source source = config.value("foo").source();
		assertNotNull(source);
		assertSame(TestBasicConfigurationBindsModule.class, source.ident);
	}
}
