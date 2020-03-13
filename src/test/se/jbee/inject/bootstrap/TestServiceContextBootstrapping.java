package se.jbee.inject.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.declare.Bundle;
import se.jbee.inject.declare.Module;

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
 */
public class TestServiceContextBootstrapping {

	@Test
	public void serviceLoaderCanBeUsedToDeclareModuleRoots() {
		Injector context = null; //FIXME
		assertNotNull(context);
		assertEquals(13, context.resolve(int.class).intValue());
		assertEquals("com.example.app.MyFirstModule",
				context.resolve(String.class));
	}
}
