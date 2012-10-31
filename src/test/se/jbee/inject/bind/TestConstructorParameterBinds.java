package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Instance.instance;
import static de.jbee.inject.Name.named;
import static de.jbee.inject.Type.raw;
import static de.jbee.inject.util.Argument.asType;
import static de.jbee.inject.util.Argument.constant;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.junit.Test;

import de.jbee.inject.Injector;
import de.jbee.inject.Instance;
import de.jbee.inject.Parameter;
import de.jbee.inject.util.Argument;
import de.jbee.inject.util.Suppliable;

/**
 * The test illustrates how to use {@link Parameter}s to give hints which resources should be
 * injected as constructor arguments.
 * 
 * Thereby it is important to notice that the list of {@linkplain Parameter}s does *not* describe
 * which {@link Constructor} to use! Hence the sequence does *not* has to match the parameter
 * sequence in the constructor definition. As long as types are not assignable a
 * {@linkplain Parameter} is tried to used as the next constructor parameter. The first assignable
 * is used.
 * 
 * @see TestDependencyParameterBinds
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public class TestConstructorParameterBinds {

	private static class Foo {

		@SuppressWarnings ( "unused" )
		Foo( String bar, Integer baz ) {
			// no use
		}
	}

	private static class Bar {

		final String foo;

		@SuppressWarnings ( "unused" )
		Bar( String foo, Integer baz ) {
			this.foo = foo;
		}
	}

	private static class Baz {

		final String foo;
		final String bar;

		@SuppressWarnings ( "unused" )
		Baz( String foo, String bar ) {
			this.foo = foo;
			this.bar = bar;

		}
	}

	private static class Qux {

		final Serializable value;
		final CharSequence sequence;

		@SuppressWarnings ( "unused" )
		Qux( Serializable value, CharSequence sequence ) {
			this.value = value;
			this.sequence = sequence;
		}
	}

	private static class ParameterConstructorBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			Instance<String> y = instance( named( "y" ), raw( String.class ) );
			bind( String.class ).to( "should not be resolved" );
			bind( named( "x" ), String.class ).to( "x" );
			bind( y ).to( "y" );
			bind( Integer.class ).to( 42 );
			bind( Foo.class ).toConstructor( raw( String.class ) );
			bind( Bar.class ).toConstructor( raw( Integer.class ), y );
			bind( Baz.class ).toConstructor( y, y );
			bind( CharSequence.class ).to( String.class ); // should not be used
			bind( Serializable.class ).to( Integer.class ); // should not be used
			bind( Qux.class ).toConstructor( asType( CharSequence.class, y ),
					constant( Number.class, 1980 ) );
		}
	}

	private static class ConsatntParameterConstructorBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( named( "const" ), Baz.class ).toConstructor(
					Argument.constant( String.class, "1st" ),
					Argument.constant( String.class, "2nd" ) );
		}

	}

	private static class WrongParameterConstructorBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Bar.class ).toConstructor( raw( Float.class ) );
		}

	}

	private final Injector injector = Bootstrap.injector( ParameterConstructorBindsModule.class );

	@Test
	public void thatClassParameterIsUnderstood() {
		assertThat( injector.resolve( dependency( Foo.class ) ), notNullValue() );
	}

	@Test
	public void thatTypeParameterIsUnderstood() {
		assertThat( injector.resolve( dependency( Bar.class ) ), notNullValue() );
	}

	@Test
	public void thatInstanceParameterIsUnderstood() {
		Bar bar = injector.resolve( dependency( Bar.class ) );
		assertThat( bar.foo, is( "y" ) );
	}

	/**
	 * We can see that {@link Argument#asType(Class, Parameter)} works because the instance y would
	 * also been assignable to the 1st parameter of type {@link Serializable} (in {@link Qux}) but
	 * since we typed it as a {@link CharSequence} it no longer is assignable to 1st argument and
	 * will be used as 2nd argument where the as well {@link Serializable} {@link Number} constant
	 * used as 2nd {@link Parameter} is used for as the 1st argument.
	 */
	@Test
	public void thatParameterAsAnotherTypeIsUnderstood() {
		Qux qux = injector.resolve( dependency( Qux.class ) );
		assertEquals( "y", qux.sequence );
	}

	/**
	 * @see #thatParameterAsAnotherTypeIsUnderstood()
	 */
	@Test
	public void thatConstantParameterIsUnderstood() {
		Qux qux = injector.resolve( dependency( Qux.class ) );
		assertEquals( 1980, qux.value );
	}

	@Test
	public void thatReoccuringTypesAreUnderstoodAsOccuringAfterAnother() {
		Baz baz = injector.resolve( dependency( Baz.class ) );
		assertThat( baz.foo, is( "y" ) );
		assertThat( "when x alignment after another is broken", baz.bar, is( "y" ) );
	}

	@Test ( expected = IllegalArgumentException.class )
	public void thatParametersNotUnderstoodThrowsException() {
		Bootstrap.injector( WrongParameterConstructorBindsModule.class );
	}

	@Test
	public void thatConstantsAreJustUsedAsConstructorArgumentsWhenPossible() {
		Suppliable<?>[] suppliables = Bootstrap.suppliables( ConsatntParameterConstructorBindsModule.class );
		assertThat( suppliables.length, is( 1 ) );
		assertTrue( suppliables[0].supplier.getClass().getSimpleName().contains( "Static" ) ); // don't want to expose class so we check for name with a string
	}
}
