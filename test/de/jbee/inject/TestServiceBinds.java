package de.jbee.inject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.service.Service;
import de.jbee.inject.service.ServiceModule;

public class TestServiceBinds {

	static class ServiceBindsModule
			extends ServiceModule {

		@Override
		protected void configure() {
			bindServices( SomeTestService.class );
		}

	}

	public static class SomeTestService {

		public Integer mul2( Integer value, Service<Float, Integer> service ) {
			return value * 2 + service.invoke( 2.8f );
		}

		public Integer round( Float value ) {
			return Math.round( value );
		}
	}

	@Test
	public void test() {
		Injector injector = Injector.create( new ServiceBindsModule(), new BuildinModuleBinder() );
		Dependency<Service> dependency = Dependency.dependency( Type.raw( Service.class ).parametized(
				Integer.class, Integer.class ) );
		Service<Integer, Integer> service = injector.resolve( dependency );
		assertNotNull( service );
		assertThat( service.invoke( 3 ), is( 9 ) );
	}
}
