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

	/**
	 * Think of this as an application specific service interface that normally would have been
	 * defined within your application code. This is what 'your' code asks for whereby you don't get
	 * any dependencies pointing in direction of the DI framework inside your normal app-code. The
	 * only place you are coupled to the DI-framework is still in the binding code that is
	 * additional to the app-code.
	 */
	public static interface AppService<P, R> {

		R doIt( P param );
	}

	/**
	 * This is an adapter to 'your' application specific service interface adapting to the
	 * {@link Service} interface of the DI-framework. So internally both are resolvable.
	 */
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

	@Test
	public void thatAppServiceCanBeResolvedWhenHavingGenericsInSameOrder() {
		DependencyResolver injector = Bootstrap.injector( ServiceDecouplerBindsModule.class );
		AppService<Integer, Integer> service = injector.resolve( Dependency.dependency( Type.raw(
				AppService.class ).parametized( Integer.class, Integer.class ) ) );
		assertThat( service.doIt( 2 ), is( 4 ) );
	}

	@Test
	public void thatAppServiceCanBeResolvedWhenHavingGenericsInDifferentOrder() {
		//TODO
	}

	@Test
	public void thatAppServiceCanBeResolvedWhenHavingJustOneGeneric() {
		//TODO
	}

}
