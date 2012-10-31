package se.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Type.raw;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.service.ServiceMethod;
import se.jbee.inject.service.ServiceModule;

public class TestServiceMethodBinds {

	private static class ServiceBindsModule
			extends ServiceModule {

		@Override
		protected void declare() {
			bindServiceMethodsIn( MyService.class );
			bindServiceMethodsIn( MyOtherService.class );
		}

	}

	static class MyService {

		public Integer negate( Number value ) {
			return -value.intValue();
		}
	}

	static class MyOtherService {

		public int mul2( int value, ServiceMethod<Float, Integer> service ) {
			return value * 2 + service.invoke( 2.8f );
		}

		public int round( float value ) {
			return Math.round( value );
		}
	}

	@Test
	public void test() {
		Injector injector = Bootstrap.injector( ServiceBindsModule.class );
		Dependency<ServiceMethod> dependency = dependency( raw( ServiceMethod.class ).parametized(
				Integer.class, Integer.class ) );
		ServiceMethod<Integer, Integer> mul2 = injector.resolve( dependency );
		assertNotNull( mul2 );
		assertThat( mul2.invoke( 3 ), is( 9 ) );
		Dependency<ServiceMethod> dependency2 = dependency( raw( ServiceMethod.class ).parametized(
				Number.class, Integer.class ) );
		ServiceMethod<Number, Integer> negate = injector.resolve( dependency2 );
		assertNotNull( mul2 );
		assertThat( negate.invoke( 3 ), is( -3 ) );
		assertThat( mul2.invoke( 4 ), is( 11 ) );
	}
}
