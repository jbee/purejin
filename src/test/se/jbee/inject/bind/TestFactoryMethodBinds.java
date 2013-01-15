package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.util.Typecast.providerTypeOf;

import java.math.BigDecimal;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.util.Provider;

public class TestFactoryMethodBinds {

	static class FactoryMethodBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( int.class ).toMethod( FactoryMethodBindsImplementor.class );
			bind( float.class ).toMethod( FactoryMethodBindsImplementor.class );
			bind( long.class ).toMethod( FactoryMethodBindsImplementor.class );
			bind( double.class ).toMethod( FactoryMethodBindsImplementor.class );
			bind( BigDecimal.class ).toMethod( FactoryMethodBindsImplementor.class );
			bind( String.class ).toModuleMethod();
		}

		static Provider<String> staticProviderFactoryMethod() {
			return new Provider<String>() {

				@Override
				public String provide() {
					return System.lineSeparator();
				}
			};
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

		Provider<BigDecimal> instanceProviderFactoryMethod() {
			return new Provider<BigDecimal>() {

				@Override
				public BigDecimal provide() {
					return new BigDecimal( System.currentTimeMillis() );
				}
			};
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

	@Test
	public void thatStaticProviderFactoryMethodIsAvailable() {
		assertEquals( System.lineSeparator(), injector.resolve(
				dependency( providerTypeOf( String.class ) ) ).provide() );
	}

	@Test
	public void thatStaticProviderFactoryMethodIsAvailableAndImplicitlyUnboxed() {
		assertEquals( System.lineSeparator(), injector.resolve( dependency( String.class ) ) );
	}

	@Test
	public void thatInstanceProviderFactoryMethodIsAvailable() {
		long before = System.currentTimeMillis();
		BigDecimal resolved = injector.resolve( dependency( providerTypeOf( BigDecimal.class ) ) ).provide();
		long after = System.currentTimeMillis();
		assertTrue( before <= resolved.longValueExact() );
		assertTrue( after >= resolved.longValueExact() );
	}
}
