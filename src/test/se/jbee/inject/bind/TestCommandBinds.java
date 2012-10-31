package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Type.raw;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.Injector;
import de.jbee.inject.Supplier;
import de.jbee.inject.service.ServiceMethod;
import de.jbee.inject.service.ServiceModule;
import de.jbee.inject.service.ServiceProvider;

/**
 * This test demonstrates that it is possible to have different higher level 'service' on top of
 * {@link ServiceMethod}s.
 * 
 * While the {@link TestServiceBinds} shows how do build a generic service this test shows a simpler
 * version {@link Command} of such generic service having a fix return type. Thereby it is very well
 * possible to use different higher level services in the same time.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public class TestCommandBinds {

	private static interface Command<P> {

		Long calc( P param );
	}

	private static class CommandSupplier
			implements Supplier<Command<?>> {

		@Override
		public Command<?> supply( Dependency<? super Command<?>> dependency, Injector injector ) {
			ServiceProvider provider = injector.resolve( dependency( ServiceProvider.class ) );
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
			public Long calc( P param ) {
				return service.invoke( param );
			}

		}
	}

	private static class CommandBindsModule
			extends ServiceModule {

		@Override
		protected void declare() {
			bindServiceMethodsIn( MathService.class );
			starbind( Command.class ).toSupplier( CommandSupplier.class );
		}

	}

	static class MathService {

		Long square( Integer value ) {
			return value.longValue() * value;
		}
	}

	@Test
	public void thatServiceCanBeResolvedWhenHavingJustOneGeneric() {
		Injector injector = Bootstrap.injector( CommandBindsModule.class );
		Dependency<Command> dependency = dependency( raw( Command.class ).parametized(
				Integer.class ) );
		Command<Integer> square = injector.resolve( dependency );
		assertThat( square.calc( 3 ), is( 9L ) );
	}
}
