package test.integration.example1;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Env;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.binder.ServiceLoaderBundles;
import se.jbee.inject.binder.ServiceLoaderEnvBundles;
import test.Example;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
class TestServiceLoaderBootstrapBinds {

	@Test
	void serviceLoaderCanBeUsedToDeclareModuleRoots() {
		Injector context = Example.EXAMPLE_1.injector();
		assertNotNull(context);
		assertEquals(13, context.resolve(int.class).intValue());
		assertEquals("test.example1.Example1Module",
				context.resolve(String.class));
	}
}
