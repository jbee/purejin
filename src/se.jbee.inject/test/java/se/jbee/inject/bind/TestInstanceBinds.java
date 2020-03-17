package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * The test demonstrates binds that are 'linked' by type.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestInstanceBinds {

	private static class Foo {

	}

	private static class InstanceBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(Number.class).to(Integer.class);
			bind(Integer.class).to(42);
			bind(Foo.class).to(Foo.class);
		}

	}

	private final Injector injector = Bootstrap.injector(
			InstanceBindsModule.class);

	@Test
	public void thatNumberDependencyIsResolvedToIntegerBoundSupplier() {
		Number number = injector.resolve(Number.class);
		assertTrue(number instanceof Integer);
		assertEquals(42, number.intValue());
	}

	@Test
	public void thatTypeLinkedBackToItselfBecomesConstructorBinding() {
		assertNotNull(injector.resolve(Foo.class));
	}
}
