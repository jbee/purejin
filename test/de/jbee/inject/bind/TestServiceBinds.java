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
	 * any dependencies pointing in direction of the DI framework inside your normal application
	 * code. The only place you are coupled to the DI-framework is still in the binding code that is
	 * additional to the application code.
	 */
	private static interface Service<P, R> {

		R doIt( P param );
	}

	/**
	 * This is an adapter to 'your' application specific service interface adapting to the
	 * {@link ServiceMethod} interface of the DI-framework. So internally both are resolvable.
	 */
	private static class ServiceSupplier
			implements Supplier<Service<?, ?>> {

		@Override
		public Service<?, ?> supply( Dependency<? super Service<?, ?>> dependency,
				DependencyResolver context ) {
			ServiceProvider provider = context.resolve( dependency( ServiceProvider.class ) );
			Type<?>[] parameters = dependency.getType().getParameters();
			return newService( provider.provide( parameters[0], parameters[1] ) );
		}

		private <P, R> Service<P, R> newService( ServiceMethod<P, R> service ) {
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
			public R doIt( P param ) {
				return service.invoke( param );
			}

		}

	}

	private static interface Command<P> {

		Long doIt( P param );
	}

	private static class CommandSupplier
			implements Supplier<Command<?>> {

		@Override
		public Command<?> supply( Dependency<? super Command<?>> dependency,
				DependencyResolver context ) {
			ServiceProvider provider = context.resolve( dependency( ServiceProvider.class ) );
			return newCommand( provider.provide( dependency.getType().getParameters()[0],
					raw( Long.class ) ) );
		}

		private <P> Command<P> newCommand( ServiceMethod<P, Long> service ) {
			return new CommandToServiceMethodAdapter<P>( service );
		}

		static class CommandToServiceMethodAdapter<P>
				implements Command<P> {

			private final ServiceMethod<P, Long> service;

			CommandToServiceMethodAdapter( ServiceMethod<P, Long> service ) {
				super();
				this.service = service;
			}

			@Override
			public Long doIt( P param ) {
				return service.invoke( param );
			}

		}
	}

	private static class ServiceBindsModule
			extends ServiceModule {

		@Override
		protected void declare() {
			bindServiceMethodsIn( MathService.class );
			superbind( Service.class ).toSupplier( ServiceSupplier.class );
			superbind( Command.class ).toSupplier( CommandSupplier.class );
		}

	}

	static class MathService {

		Long square( Integer value ) {
			return value.longValue() * value;
		}
	}

	@Test
	public void thatServiceCanBeResolvedWhenHavingGenericsInSameOrder() {
		DependencyResolver injector = Bootstrap.injector( ServiceBindsModule.class );
		Dependency<Service> dependency = dependency( raw( Service.class ).parametized(
				Integer.class, Long.class ) );
		Service<Integer, Long> service = injector.resolve( dependency );
		assertThat( service.doIt( 2 ), is( 4L ) );
	}

	@Test
	public void thatServiceCanBeResolvedWhenHavingGenericsInDifferentOrder() {
		//TODO
	}

	@Test
	public void thatServiceCanBeResolvedWhenHavingJustOneGeneric() {
		DependencyResolver injector = Bootstrap.injector( ServiceBindsModule.class );
		Dependency<Command> dependency = dependency( raw( Command.class ).parametized(
				Integer.class ) );
		Command<Integer> command = injector.resolve( dependency );
		assertThat( command.doIt( 3 ), is( 9L ) );
	}

}
