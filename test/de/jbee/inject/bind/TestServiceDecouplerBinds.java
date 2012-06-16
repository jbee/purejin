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
	static interface AppService<P, R> {

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

	static interface AppCommand<P> {

		Long doIt( P param );
	}

	static class AppCommandDecoupler
			implements Service.ServiceDecoupler<AppCommand<?>> {

		@SuppressWarnings ( "unchecked" )
		@Override
		public <P, R> AppCommand<?> decouple( Service<P, R> service, Type<R> returnType,
				Type<P> parameterType ) {
			return new AppCommandAdapter<P>( (Service<P, Long>) service );
		}

		static class AppCommandAdapter<P>
				implements AppCommand<P> {

			private final Service<P, Long> service;

			AppCommandAdapter( Service<P, Long> service ) {
				super();
				this.service = service;
			}

			@Override
			public Long doIt( P param ) {
				return service.invoke( param );
			}

		}
	}

	static class ServiceDecouplerBindsModule
			extends ServiceModule {

		@Override
		protected void configure() {
			bindServices( SomeService.class );
			bind( AppService.class, AppServiceDecoupler.class );
			bind( AppCommand.class, AppCommandDecoupler.class );
		}

	}

	static class SomeService {

		Long square( Integer value ) {
			return value.longValue() * value;
		}
	}

	@Test
	public void thatAppServiceCanBeResolvedWhenHavingGenericsInSameOrder() {
		DependencyResolver injector = Bootstrap.injector( ServiceDecouplerBindsModule.class );
		AppService<Integer, Long> service = injector.resolve( Dependency.dependency( Type.raw(
				AppService.class ).parametized( Integer.class, Long.class ) ) );
		assertThat( service.doIt( 2 ), is( 4L ) );
	}

	@Test
	public void thatAppServiceCanBeResolvedWhenHavingGenericsInDifferentOrder() {
		//TODO
	}

	@Test
	public void thatAppServiceCanBeResolvedWhenHavingJustOneGeneric() {
		DependencyResolver injector = Bootstrap.injector( ServiceDecouplerBindsModule.class );
		AppCommand<Integer> command = injector.resolve( Dependency.dependency( Type.raw(
				AppCommand.class ).parametized( Integer.class ) ) );
		assertThat( command.doIt( 3 ), is( 9L ) );
	}

}
