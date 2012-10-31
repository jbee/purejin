package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.util.Scoped.TARGET_INSTANCE;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.Injector;
import de.jbee.inject.Instance;
import de.jbee.inject.Name;

public class TestRobotLegsProblemBinds {

	private static class Foot {
		// the left and right foot
	}

	private static class Leg {

		final Foot foot;

		@SuppressWarnings ( "unused" )
		Leg( Foot foot ) {
			this.foot = foot;

		}
	}

	static Name left = Name.named( "left" );
	static Name right = Name.named( "right" );

	/**
	 * left and right {@link Foot} could be explicitly bind to left or right {@link Leg}.
	 */
	private static class RobotLegsProblemBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( left, Leg.class ).toConstructor();
			bind( right, Leg.class ).toConstructor();
			injectingInto( left, Leg.class ).bind( Foot.class ).to( left, Foot.class );
			injectingInto( right, Leg.class ).bind( Foot.class ).to( right, Foot.class );
		}
	}

	/**
	 * Or generally there should be one {@link Foot} for each {@link Instance} one is injected into.
	 */
	private static class RobotLegsProblemScopeBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			per( TARGET_INSTANCE ).construct( Foot.class );
			bind( left, Leg.class ).toConstructor();
			bind( right, Leg.class ).toConstructor();
		}
	}

	@Test
	public void thatRobotHasDifferentLegsWhenUsingInjectingIntoClause() {
		assertRobotHasDifferentLegsWithDifferentFoots( Bootstrap.injector( RobotLegsProblemBindsModule.class ) );
	}

	@Test
	public void thatRobotHasDifferentLegsWhenUsingTargetInstanceScopedFeets() {
		assertRobotHasDifferentLegsWithDifferentFoots( Bootstrap.injector( RobotLegsProblemScopeBindsModule.class ) );
	}

	private void assertRobotHasDifferentLegsWithDifferentFoots( Injector injector ) {
		Leg leftLeg = injector.resolve( dependency( Leg.class ).named( left ) );
		Leg rightLeg = injector.resolve( dependency( Leg.class ).named( right ) );
		assertThat( "same leg", leftLeg, not( sameInstance( rightLeg ) ) );
		assertThat( "same foot", leftLeg.foot, not( sameInstance( rightLeg.foot ) ) );
	}
}
