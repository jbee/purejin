package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Instance.instance;
import static de.jbee.inject.Name.named;
import static de.jbee.inject.Type.raw;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Instance;

public class TestHintConstructorBinds {

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

	private static class HintConstructorBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( named( "x" ), String.class ).to( "x" );
			bind( named( "y" ), String.class ).to( "y" );
			bind( Integer.class ).to( 42 );
			bind( Foo.class ).toConstructor( String.class );
			Instance<String> y = instance( named( "y" ), raw( String.class ) );
			bind( Bar.class ).toConstructor( raw( Integer.class ), y );
			bind( Baz.class ).toConstructor( y, y );
		}
	}

	private static class WrongHintConstructorBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Bar.class ).toConstructor( Float.class );
		}

	}

	private final DependencyResolver injector = Bootstrap.injector( HintConstructorBindsModule.class );

	@Test
	public void thatClassHintIsUnderstood() {
		assertThat( injector.resolve( dependency( Foo.class ) ), notNullValue() );
	}

	@Test
	public void thatTypeHintIsUnderstood() {
		assertThat( injector.resolve( dependency( Bar.class ) ), notNullValue() );
	}

	@Test
	public void thatInstanceHintIsUnderstood() {
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
	public void thatHintNotUnderstoodThrowsException() {
		Bootstrap.injector( WrongHintConstructorBindsModule.class );
	}
}
