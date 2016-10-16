package se.jbee.inject.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.action.ActionModule.actionDependency;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;

public class TestActionBinds {

	private static class ActionBindsModule
			extends ActionModule {

		@Override
		protected void declare() {
			bindActionsIn( MyService.class );
			bindActionsIn( MyOtherService.class );
		}

	}

	static class MyService {

		public Integer negate( Number value ) {
			return -value.intValue();
		}
	}

	static class MyOtherService {

		public int mul2( int value, Action<Float, Integer> service ) {
			return value * 2 + service.exec( 2.8f );
		}

		public int round( float value ) {
			return Math.round( value );
		}
	}

	@Test
	public void actionsDecoupleConcreteMethods() {
		Injector injector = Bootstrap.injector( ActionBindsModule.class );
		Dependency<Action<Integer, Integer>> p1 = actionDependency(raw(Integer.class), raw(Integer.class));
		Action<Integer, Integer> mul2 = injector.resolve( p1 );
		assertNotNull( mul2 );
		assertEquals( 9, mul2.exec( 3 ).intValue() );
		Dependency<Action<Number, Integer>> p2 = actionDependency(raw(Number.class), raw(Integer.class));
		Action<Number, Integer> negate = injector.resolve( p2 );
		assertNotNull( mul2 );
		assertEquals( -3, negate.exec( 3 ).intValue() );
		assertEquals( 11, mul2.exec( 4 ).intValue() );
	}
}
