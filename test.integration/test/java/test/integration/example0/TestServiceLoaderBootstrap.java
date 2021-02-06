package test.integration.example0;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Small test to check that {@link Bootstrap#currentEnv()} and {@link
 * Bootstrap#currentInjector()} do the expected thing.
 */
class TestServiceLoaderBootstrap {

	@Test
	void serviceLoaderEnvIsOnlyCreatedOnce() {
		assertSame(Bootstrap.currentEnv(), Bootstrap.currentEnv());
	}

	@Test
	void serviceLoaderInjectorIsOnlyCreatedOnce() {
		assertSame(Bootstrap.currentInjector(), Bootstrap.currentInjector());
	}

	@Test
	void serviceLoaderInjectorUsesServiceLoaderEnv() {
		assertSame(Bootstrap.currentEnv(), Bootstrap.currentInjector().resolve(
				Env.class));
	}
}
