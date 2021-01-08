package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Scope;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * This tests demonstrates 2 different ways to solve the somewhat famous "Robot
 * legs problem" (google it).
 * <p>
 * This problem illustrates a common difficulty in dependency injection with the
 * help of a virtual robot and its parts. The task is to compose the robot, in
 * particular its two legs, naturally each having its own foot.
 * <p>
 * The parts should be represented as classes {@link Foot} and {@link Leg}.
 * <p>
 * Essentially the problem tasks the dependency injection to use composition
 * over inheritance. To model the robots legs and feet in the way typically used
 * in object orientation where we simply instantiate a class multiple times to
 * create both left and right feet and legs instances and pass the correct
 * reference to corresponding receiver instance.
 * <p>
 * Surprisingly, many dependency injection frameworks and libraries fail this
 * very simple task or require the user to perform contortions and introduce
 * more types which illustrates the mismatch of their concepts to build software
 * that is composed in the very same way we would write it without dependency
 * injection.
 * <p>
 * So in some sense this can be said to be the litmus test of dependency
 * injection. I hope you agree that both {@link Solution1} and {@link Solution2}
 * shown here are not just simple but quite elegant and intuitive too.
 * <p>
 * I dare say the robot legs problem poses no problem at all to the concepts of
 * this library. It can hardly be called a special case and only scratches on
 * the surface of what can be expressed when it comes to the relations between
 * instances and how to wire them together.
 * <p>
 * To those thinking that they have not needed composition from their dependency
 * injection library I say:
 * <blockquote>
 * The limits of my language mean the limits of my world -- Wittgenstein
 * </blockquote>
 */
class TestExampleRobotLegsProblemBinds {

	public static class Foot {
		// the left and right foot
	}

	public static class Leg {

		final Foot foot;

		public Leg(Foot foot) {
			this.foot = foot;

		}
	}

	/**
	 * Each {@link Foot} could be explicitly bound to its respective {@link
	 * Leg}.
	 */
	private static class Solution1 extends BinderModule {

		@Override
		protected void declare() {
			bind("left", Leg.class).toConstructor();
			bind("right", Leg.class).toConstructor();
			injectingInto("left", Leg.class).bind(Foot.class).to("left", Foot.class);
			injectingInto("right", Leg.class).bind(Foot.class).to("right", Foot.class);
		}
	}

	/**
	 * Or a general statement can be made that a {@link Foot} is created for
	 * each {@link Instance} one is needed for. As there are two distinct {@link
	 * Leg}s each gets its own {@link Foot}.
	 */
	private static class Solution2 extends BinderModule {

		@Override
		protected void declare() {
			bind("left", Leg.class).toConstructor();
			bind("right", Leg.class).toConstructor();
			per(Scope.targetInstance).construct(Foot.class);
		}
	}

	@Test
	void robotHasDifferentLegsWhenUsingInjectingIntoClause() {
		assertRobotHasDifferentLegsWithDifferentFeet(
				Bootstrap.injector(Solution1.class));
	}

	@Test
	void robotHasDifferentLegsWhenUsingTargetInstanceScopedFeet() {
		assertRobotHasDifferentLegsWithDifferentFeet(
				Bootstrap.injector(Solution2.class));
	}

	private static void assertRobotHasDifferentLegsWithDifferentFeet(
			Injector context) {
		Leg leftLeg = context.resolve("left", Leg.class);
		Leg rightLeg = context.resolve("right", Leg.class);
		assertNotSame(rightLeg, leftLeg, "same leg");
		assertNotSame(rightLeg.foot, leftLeg.foot, "same foot");
	}
}
