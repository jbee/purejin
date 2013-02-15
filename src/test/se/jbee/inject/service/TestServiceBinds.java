package se.jbee.inject.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Type.raw;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrap;

public class TestServiceBinds {

	/**
	 * Think of this as an application specific service interface that normally would have been
	 * defined within your application code. This is what 'your' code asks for whereby you don't get
	 * any dependencies pointing in direction of the DI framework inside your normal application
	 * code. The only place you are coupled to the DI-framework is still in the binding code that is
	 * additional to the application code.
	 */
	private static interface Service<P, R> {

		R calc( P param );
	}

	/**
	 * This is an adapter to 'your' application specific service interface adapting to the
	 * {@link ServiceMethod} interface of the DI-framework. So internally both are resolvable.
	 */
	private static class ServiceSupplier
			implements Supplier<Service<?, ?>> {

		@Override
		public Service<?, ?> supply( Dependency<? super Service<?, ?>> dependency, Injector injector ) {
			ServiceProvider provider = injector.resolve( dependency( ServiceProvider.class ) );
			Type<? super Service<?, ?>> type = dependency.getType();
			return newService( provider.provide( type.parameter( 0 ), type.parameter( 1 ) ) );
		}

		private static <P, R> Service<P, R> newService( ServiceMethod<P, R> service ) {
			return new ServiceToServiceMethodAdapter<P, R>( service );
		}

		static class ServiceToServiceMethodAdapter<P, R>
				implements Service<P, R> {

			private final ServiceMethod<P, R> service;

			ServiceToServiceMethodAdapter( ServiceMethod<P, R> service ) {
				super();
				this.service = service;
			}

			@Override
			public R calc( P param ) {
				return service.invoke( param );
			}

		}

	}

	private static class ServiceBindsModule
			extends ServiceModule {

		@Override
		protected void declare() {
			bindServiceMethodsIn( MathService.class );
			starbind( Service.class ).toSupplier( ServiceSupplier.class );
		}

	}

	static class MathService {

		Long square( Integer value ) {
			return value.longValue() * value;
		}
	}

	@Test
	public void thatServiceCanBeResolvedWhenHavingGenericsInSameOrder() {
		Injector injector = Bootstrap.injector( ServiceBindsModule.class );
		@SuppressWarnings ( { "rawtypes" } )
		Dependency<Service> dependency = dependency( raw( Service.class ).parametized(
				Integer.class, Long.class ) );
		@SuppressWarnings ( "unchecked" )
		Service<Integer, Long> square = injector.resolve( dependency );
		assertThat( square.calc( 2 ), is( 4L ) );
	}

}
