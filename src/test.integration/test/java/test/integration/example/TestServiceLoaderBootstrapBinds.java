package test.integration.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.ServiceLoaderBundles;
import se.jbee.inject.binder.ServiceLoaderEnvBundles;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bind.Bundle;

/**
 * This test demonstrates how the {@link java.util.ServiceLoader} concept is
 * used to define one or more root {@link Bundle} {@link Class}es as
 * <code>/META-INF/services/se.jbee.inject.bootstrap.Bundle</code> files in jar
 * files to assemble the context of an modular application.
 *
 * This example is based on the <code>/lib/example.jar</code> which contains
 * very basic example of one {@link Bundle} installing one {@link Module} which
 * is binding an int and {@link String} value. These example files can be found
 * in <code>src/example</code>.
 *
 * The {@link ServiceLoader} as a source is hooked in explicitly by installing
 * the {@link ServiceLoaderEnvBundles} when bootstrapping an {@link Env} and the
 * {@link ServiceLoaderBundles} when bootstrapping an {@link Injector}. This
 * gives same control as always for this feature as well.
 */
public class TestServiceLoaderBootstrapBinds {

	@Test
	public void serviceLoaderCanBeUsedToDeclareModuleRoots() {
		Env env = Bootstrap.env(ServiceLoaderEnvBundles.class);
		Injector context = Bootstrap.injector(env, ServiceLoaderBundles.class);
		assertNotNull(context);
		assertEquals(13, context.resolve(int.class).intValue());
		assertEquals("com.example.app.MyFirstModule",
				context.resolve(String.class));
	}
}
