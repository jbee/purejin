package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.Serializable;

import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Supplier;

/**
 * The tests demonstrates the meaning of a {@link Binder#autobind(Class)} call. That will create
 * multiple binds, one each for the type and all its super-classes and -interfaces. All of them are
 * bound to the same to-clause, hence share the same {@link Supplier} (in the end all to-clauses
 * become one).
 * 
 * This is special since we always ask for an explicitly bound type when resolving a
 * {@link Dependency}. That means just using {@link Binder#bind(Class)} just makes the {@link Class}
 * resolvable passed to the bind method. Usually this is what we want and need to gain a predictable
 * setup. But in some cases an instance should serve as many different interfaces all implemented by
 * it (e.g. a class implementing a couple of single service interfaces).
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public class TestAutobindBinds {

	static class AutobindBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			autobind( Integer.class ).to( 42 );
		}

	}

	private final DependencyResolver injector = Bootstrap.injector( AutobindBindsModule.class );

	@Test
	public void thatTheAutoboundTypeItselfIsBound() {
		assertThat( injector.resolve( dependency( Integer.class ) ), is( 42 ) );
	}

	@Test
	public void thatDirectSuperclassOfAutoboundTypeIsBound() {
		assertThat( injector.resolve( dependency( Number.class ) ).intValue(), is( 42 ) );
	}

	@Test
	public void thatSuperinterfaceOfAutoboundTypeIsBound() {
		assertEquals( injector.resolve( dependency( Serializable.class ) ), 42 );
	}

	//TODO compareable<Integer>
}
