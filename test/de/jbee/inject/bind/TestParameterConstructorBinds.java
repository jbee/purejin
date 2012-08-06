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

import java.io.Serializable;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Instance;

public class TestParameterConstructorBinds {

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
			bind( named( "x" ), String.class ).to( "x" );
			bind( y ).to( "y" );
			bind( Integer.class ).to( 42 );
			bind( Foo.class ).toConstructor( raw( String.class ) );
			bind( Bar.class ).toConstructor( raw( Integer.class ), y );
			bind( Baz.class ).toConstructor( y, y );
			bind( CharSequence.class ).to( "should not be resolved" );
			bind( Serializable.class ).to( Integer.class );
			bind( Qux.class ).toConstructor( asType( CharSequence.class, y ),
					constant( Number.class, 1980 ) );
		}
	}

	private static class WrongParameterConstructorBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Bar.class ).toConstructor( raw( Float.class ) );
		}

	}

	private final DependencyResolver injector = Bootstrap.injector( ParameterConstructorBindsModule.class );

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

	@Test
	public void thatReoccuringTypesAreUnderstoodAsOccuringAfterAnother() {
		Baz baz = injector.resolve( dependency( Baz.class ) );
		assertThat( baz.foo, is( "y" ) );
		assertThat( "when x alignment after another is broken", baz.bar, is( "y" ) );
	}

	@Test ( expected = IllegalArgumentException.class )
	public void thatParameterNotUnderstoodThrowsException() {
		Bootstrap.injector( WrongParameterConstructorBindsModule.class );
	}

	@Test
	public void thatAsTypeAndConstantParameterIsUnderstood() {
		Qux qux = injector.resolve( dependency( Qux.class ) );
		assertEquals( 1980, qux.value );
		assertEquals( "y", qux.sequence );
	}
}
