package se.jbee.inject.service;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Dependency.dependency;

import javax.annotation.Resource;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Inspect;
import se.jbee.inject.service.ServiceMethod;
import se.jbee.inject.service.ServiceModule;

public class TestServiceInspectorBinds {

	private static class TestServiceInspectorModule
			extends ServiceModule {

		@Override
		protected void declare() {
			bindServiceInspectorTo( Inspect.all().methods().annotatedWith( Resource.class ) );
			bindServiceMethodsIn( ServiceInspectorBindsServices.class );
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
	public void thatTheDefaultServiceInspectorCanBeReplacedThroughCustomBind() {
		@SuppressWarnings ( "unchecked" )
		ServiceMethod<Void, Integer> answer = injector.resolve( dependency( Type.raw(
				ServiceMethod.class ).parametized( Void.class, Integer.class ) ) );
		assertEquals( 42, answer.invoke( null ).intValue() );
	}
}
