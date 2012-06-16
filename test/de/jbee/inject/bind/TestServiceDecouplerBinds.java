package de.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Type;
import de.jbee.inject.service.Service;
import de.jbee.inject.service.ServiceModule;

public class TestServiceDecouplerBinds {

	static class ServiceDecouplerBindsModule
			extends ServiceModule {

		@Override
		protected void configure() {
			bindServices( SomeService.class );
			bind( AppService.class, new AppServiceDecoupler() );
		}

	}

	private static class SomeService {

		Integer square( Integer value ) {
			return value * value;
		}
	}

	public static interface AppService<P, R> {

		R doIt( P param );
	}

	static class AppServiceDecoupler
			implements Service.ServiceDecoupler<AppService<?, ?>> {

		@Override
		public <P, R> AppService<?, ?> decouple( Service<P, R> service, Type<R> returnType,
				Type<P> parameterType ) {
			return new AppServiceAdapter<P, R>( service );
		}

		static class AppServiceAdapter<P, R>
				implements AppService<P, R> {

			private final Service<P, R> service;

			AppServiceAdapter( Service<P, R> service ) {
				super();
				this.service = service;
			}

			@Override
			public R doIt( P param ) {
				return service.invoke( param );
			}

		}
	}

	@Test
	public void test() {
		DependencyResolver injector = Bootstrap.injector( ServiceDecouplerBindsModule.class );
		AppService<Integer, Integer> service = injector.resolve( Dependency.dependency( Type.raw(
				AppService.class ).parametized( Integer.class, Integer.class ) ) );
		assertThat( service.doIt( 2 ), is( 4 ) );
	}
}
