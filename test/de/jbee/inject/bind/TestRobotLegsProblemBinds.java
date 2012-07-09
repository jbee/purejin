package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;
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

	// per parent/target instance scope would make it also very easy
	private static class RobotLegsProblemBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			Name left = Name.named( "left" );
			Name right = Name.named( "right" );
			bind( left, Leg.class ).toConstructor();
			bind( right, Leg.class ).toConstructor();
			injectingInto( left, Leg.class ).bind( Foot.class ).to( left, Foot.class );
			injectingInto( right, Leg.class ).bind( Foot.class ).to( right, Foot.class );
		}
	}

	@Test
	public void test() {
		DependencyResolver injector = Bootstrap.injector( RobotLegsProblemBindsModule.class );
		Leg leftLeg = injector.resolve( dependency( Leg.class ).named( "left" ) );
		Leg rightLeg = injector.resolve( dependency( Leg.class ).named( "right" ) );
		assertThat( leftLeg, not( sameInstance( rightLeg ) ) );
		assertThat( leftLeg.foot, not( sameInstance( rightLeg.foot ) ) );
	}
}
