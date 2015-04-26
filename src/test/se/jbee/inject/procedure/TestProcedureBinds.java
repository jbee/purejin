package se.jbee.inject.procedure;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.procedure.ProcedureModule.procedureDependency;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;

public class TestProcedureBinds {

	private static class ProcedureBindsModule
			extends ProcedureModule {

		@Override
		protected void declare() {
			bindProceduresIn( MyService.class );
			bindProceduresIn( MyOtherService.class );
		}

	}

	static class MyService {

		public Integer negate( Number value ) {
			return -value.intValue();
		}
	}

	static class MyOtherService {

		public int mul2( int value, Procedure<Float, Integer> service ) {
			return value * 2 + service.run( 2.8f );
		}

		public int round( float value ) {
			return Math.round( value );
		}
	}

	@Test
	public void thatServicesAbstractUsualConcreteMethods() {
		Injector injector = Bootstrap.injector( ProcedureBindsModule.class );
		Dependency<Procedure<Integer, Integer>> p1 = procedureDependency(raw(Integer.class), raw(Integer.class));
		Procedure<Integer, Integer> mul2 = injector.resolve( p1 );
		assertNotNull( mul2 );
		assertThat( mul2.run( 3 ), is( 9 ) );
		Dependency<Procedure<Number, Integer>> p2 = procedureDependency(raw(Number.class), raw(Integer.class));
		Procedure<Number, Integer> negate = injector.resolve( p2 );
		assertNotNull( mul2 );
		assertThat( negate.run( 3 ), is( -3 ) );
		assertThat( mul2.run( 4 ), is( 11 ) );
	}
}
