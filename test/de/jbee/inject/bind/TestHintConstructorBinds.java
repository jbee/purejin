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

public class TestHintConstructorBinds {

	private static class Foo {

		@SuppressWarnings ( "unused" )
		Foo( String bar, Integer baz ) {

		}
	}

	private static class Bar {

		final String foo;

		@SuppressWarnings ( "unused" )
		Bar( String foo, Integer baz ) {
			this.foo = foo;
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
			bind( Bar.class ).toConstructor( raw( Integer.class ),
					instance( named( "y" ), raw( String.class ) ) );
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
}
