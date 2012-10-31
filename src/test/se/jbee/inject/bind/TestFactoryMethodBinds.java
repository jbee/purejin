package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Dependency.dependency;

import org.junit.Test;

import se.jbee.inject.Injector;

public class TestFactoryMethodBinds {

	private static class FactoryMethodBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( int.class ).toMethod( FactoryMethodBindsImplementor.class );
			bind( float.class ).toMethod( FactoryMethodBindsImplementor.class );
			bind( long.class ).toMethod( FactoryMethodBindsImplementor.class );
			bind( double.class ).toMethod( FactoryMethodBindsImplementor.class );
		}

	}

	static class FactoryMethodBindsImplementor {

		static int staticFactoryMethod() {
			return 42;
		}

		static long staticFactoryMethodWithParameters( int factor ) {
			return factor * 2L;
		}

		float instanceFactoryMethod() {
			return 42f;
		}

		double instanceFactoryMethodWithParameters( float factor ) {
			return factor * 2d;
		}
	}

	private final Injector injector = Bootstrap.injector( FactoryMethodBindsModule.class );

	@Test
	public void thatInstanceFactoryMethodIsAvailable() {
		assertEquals( 42f, injector.resolve( dependency( float.class ) ).floatValue(), 0.01f );
	}

	@Test
	public void thatStaticFactoryMethodIsAvailable() {
		assertEquals( 42, injector.resolve( dependency( int.class ) ).intValue() );
	}

	@Test
	public void thatInstanceFactoryMethodWithParametersIsAvailable() {
		assertEquals( 84d, injector.resolve( dependency( double.class ) ).doubleValue(), 0.01d );
	}

	@Test
	public void thatStaticFactoryMethodWithParametersIsAvailable() {
		assertEquals( 84L, injector.resolve( dependency( long.class ) ).longValue() );
	}
}
