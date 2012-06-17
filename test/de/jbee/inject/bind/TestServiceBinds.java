package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Type.raw;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.DependencyResolver;
import de.jbee.inject.Supplier;
import de.jbee.inject.Type;
import de.jbee.inject.service.ServiceMethod;
import de.jbee.inject.service.ServiceModule;
import de.jbee.inject.service.ServiceProvider;

public class TestServiceBinds {

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
	 * {@link ServiceMethod} interface of the DI-framework. So internally both are resolvable.
	 */
	static class AppServiceSupplier
			implements Supplier<AppService<?, ?>> {

		@Override
		public AppService<?, ?> supply( Dependency<? super AppService<?, ?>> dependency,
				DependencyResolver context ) {
			ServiceProvider provider = context.resolve( dependency( ServiceProvider.class ) );
			Type<?>[] parameters = dependency.getType().getParameters();
			return newService( provider.provide( parameters[0], parameters[1], context ) );
		}

		private <P, R> AppService<P, R> newService( ServiceMethod<P, R> service ) {
			return new AppServiceImpl<P, R>( service );
		}

		static class AppServiceImpl<P, R>
				implements AppService<P, R> {

			private final ServiceMethod<P, R> service;

			AppServiceImpl( ServiceMethod<P, R> service ) {
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

	static class AppCommandSupplier
			implements Supplier<AppCommand<?>> {

		@Override
		public AppCommand<?> supply( Dependency<? super AppCommand<?>> dependency,
				DependencyResolver context ) {
			ServiceProvider provider = context.resolve( dependency( ServiceProvider.class ) );
			return newCommand( provider.provide( dependency.getType().getParameters()[0],
					raw( Long.class ), context ) );
		}

		private <P> AppCommand<P> newCommand( ServiceMethod<P, Long> service ) {
			return new AppCommandImpl<P>( service );
		}

		static class AppCommandImpl<P>
				implements AppCommand<P> {

			private final ServiceMethod<P, Long> service;

			AppCommandImpl( ServiceMethod<P, Long> service ) {
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
			bindServiceMethods( SomeService.class );
			superbind( AppService.class ).toSupplier( AppServiceSupplier.class );
			superbind( AppCommand.class ).toSupplier( AppCommandSupplier.class );
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
