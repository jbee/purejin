package se.jbee.inject.procedure;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.procedure.ProcedureModule.procedureDependency;

import javax.annotation.Resource;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.bootstrap.Inspector;

/**
 * The tests illustrates how to change the way service methods are identified by
 * binding a custom {@link Inspector} for services using
 * {@link ProcedureModule#discoverProceduresBy(Inspector)}.
 */
public class TestProcedureInspectorBinds {

	private static class TestServiceInspectorModule
			extends ProcedureModule {

		@Override
		protected void declare() {
			discoverProceduresBy( Inspect.all().methods().annotatedWith( Resource.class ) );
			bindProceduresIn( ServiceInspectorBindsServices.class );
		}

	}

	static class ServiceInspectorBindsServices {

		int notBoundSinceNotAnnotated() {
			return 13;
		}

		@Resource
		int aServiceProducingInts() {
			return 42;
		}
	}

	private final Injector injector = Bootstrap.injector( TestServiceInspectorModule.class );

	@Test
	public void thatTheDefaultInspectorCanBeReplacedThroughCustomBind() {
		Procedure<Void, Integer> answer = injector.resolve( procedureDependency(raw( Void.class), raw(Integer.class ) ) );
		assertEquals( 42, answer.run( null ).intValue() );
	}
}
