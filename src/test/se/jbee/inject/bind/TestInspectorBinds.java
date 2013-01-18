package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Dependency.dependency;

import org.junit.Test;

import se.jbee.inject.Injector;

public class TestInspectorBinds {

	static class FactoryMethodBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Inspected.METHODS ).inModule();
			bind( Inspected.METHODS ).in( InspectorBindsImplementor.class );
		}

		static int staticFactoryMethod() {
			return 42;
		}

		static long staticFactoryMethodWithParameters( int factor ) {
			return factor * 2L;
		}
	}

	static class InspectorBindsImplementor {

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
