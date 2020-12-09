package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Supplier;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.lang.Type.raw;

/**
 * The tests demonstrates the meaning of a {@link Binder#autobind(Class)} call.
 * That will create multiple binds, one each for the type and all its
 * super-classes and -interfaces. All of them are bound to the same to-clause,
 * hence share the same {@link Supplier} (in the end all to-clauses become one).
 *
 * This is special since we always ask for an explicitly bound type when
 * resolving a {@link Dependency}. That means just using
 * {@link Binder#bind(Class)} just makes the {@link Class} resolvable passed to
 * the bind method. Usually this is what we want and need to gain a predictable
 * setup. But in some cases an instance should serve as many different
 * interfaces all implemented by it (e.g. a class implementing a couple of
 * single service interfaces).
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
class TestAutobindBinds {

	static class AutobindBindsModule extends BinderModule {

		@Override
		protected void declare() {
			autobind(Integer.class).to(42);
			autobind(raw(List.class).parametized(String.class)).to(
					Arrays.asList(new String[] {}));
		}

	}

	private final Injector injector = Bootstrap.injector(
			AutobindBindsModule.class);

	@Test
	public void thatTheAutoboundTypeItselfIsBound() {
		assertEquals(42, injector.resolve(Integer.class).intValue());
	}

	@Test
	public void thatDirectSuperclassOfAutoboundTypeIsBound() {
		assertEquals(42, injector.resolve(Number.class).intValue());
	}

	@Test
	public void thatSuperinterfaceOfAutoboundTypeIsBound() {
		assertEquals(42, injector.resolve(Serializable.class));
	}

	@Test
	public void thatParametizedSuperinterfaceOfAutoboundTypeIsBound() {
		assertEquals(42, injector.resolve(
				raw(Comparable.class).parametized(Integer.class)));
	}

}
