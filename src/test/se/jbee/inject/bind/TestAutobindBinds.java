package se.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Type.raw;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Supplier;
import se.jbee.inject.bootstrap.Bootstrap;

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
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestAutobindBinds {

	static class AutobindBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			autobind( Integer.class ).to( 42 );
			autobind( raw( List.class ).parametized( String.class ) ).to(
					Arrays.asList( new String[] {} ) );
		}

	}

	private final Injector injector = Bootstrap.injector( AutobindBindsModule.class );

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

	@Test
	public void thatParametizedSuperinterfaceOfAutoboundTypeIsBound() {
		assertEquals( injector.resolve( dependency( raw( Comparable.class ).parametized(
				Integer.class ) ) ), 42 );
	}

}
