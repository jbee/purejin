package se.jbee.inject.procedure;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.container.Scoped.DEPENDENCY_TYPE;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Supplier;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.procedure.Procedure;
import se.jbee.inject.procedure.ProcedureModule;

/**
 * This test demonstrates that it is possible to have different higher level 'service' on top of
 * {@link Procedure}s.
 * 
 * While the {@link TestServiceBinds} shows how do build a generic service this test shows a simpler
 * version {@link Command} of such generic service having a fix return type. Thereby it is very well
 * possible to use different higher level services in the same time.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestCommandBinds {

	private static interface Command<P> {

		Long calc( P param );
	}

	private static class CommandSupplier
			implements Supplier<Command<?>> {

		@Override
		public Command<?> supply( Dependency<? super Command<?>> dependency, Injector injector ) {
			return newCommand( injector.resolve( ProcedureModule.procedureDependency(dependency.type().parameter( 0 ), raw( Long.class )) ) );
		}
		
		private static <P> Command<P> newCommand( Procedure<P, Long> service ) {
			return new CommandToServiceMethodAdapter<P>( service );
		}

		static class CommandToServiceMethodAdapter<P>
				implements Command<P> {

			private final Procedure<P, Long> service;

			CommandToServiceMethodAdapter( Procedure<P, Long> service ) {
				super();
				this.service = service;
			}

			@Override
			public Long calc( P param ) {
				return service.run( param );
			}

		}
	}

	private static class CommandBindsModule
			extends ProcedureModule {

		@Override
		protected void declare() {
			bindProceduresIn( MathService.class );
			per( DEPENDENCY_TYPE ).starbind( Command.class ).toSupplier( CommandSupplier.class );
		}

	}

	static class MathService {

		Long square( Integer value ) {
			return value.longValue() * value;
		}
	}

	@SuppressWarnings ( "unchecked" )
	@Test
	public void thatServiceCanBeResolvedWhenHavingJustOneGeneric() {
		Injector injector = Bootstrap.injector( CommandBindsModule.class );
		@SuppressWarnings ( "rawtypes" )
		Dependency<Command> dependency = dependency( raw( Command.class ).parametized(
				Integer.class ) );
		Command<Integer> square = injector.resolve( dependency );
		assertThat( square.calc( 3 ), is( 9L ) );
	}
}
