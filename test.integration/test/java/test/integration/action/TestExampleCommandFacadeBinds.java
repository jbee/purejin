package test.integration.action;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.ProducesBy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.jbee.inject.action.Action.actionTypeOf;
import static se.jbee.lang.Type.raw;

/**
 * This test demonstrates the {@link Action} concept can be used for higher
 * level 'service' abstractions with different contract.
 * <p>
 * While the {@link TestExampleServiceFacadeBinds} shows how do build a generic
 * service this test shows a simpler version {@link Command} of such generic
 * service having a fix return type.
 * <p>
 * Note that possible to use different higher level service facades on top of
 * {@link Action} in the same time as these only delegate based on matching type
 * signatures.
 *
 * @see TestExampleServiceFacadeBinds
 */
class TestExampleCommandFacadeBinds {

	@FunctionalInterface
	private interface Command<P> {

		long calc(P param);
	}

	private static class CommandModule extends ActionModule {

		@Override
		protected void declare() {
			construct(MathService.class);
			connect(ProducesBy.OPTIMISTIC).inAny(MathService.class).asAction();
			per(Scope.dependencyType) //
					.starbind(Command.class) //
					.toSupplier(CommandModule::supply);
		}

		private static Command<?> supply(Dependency<? super Command<?>> dep,
				Injector context) {
			return newCommand(context.resolve(actionTypeOf(
					dep.type().parameter(0), raw(long.class))));
		}

		private static <P> Command<P> newCommand(Action<P, Long> action) {
			return action::run;
		}
	}

	public static class MathService {

		public Long square(Integer value) {
			return value.longValue() * value;
		}
	}

	private final Injector context = Bootstrap.injector(CommandModule.class);

	@Test
	void commandBecomesUserFacadeForAction() {
		@SuppressWarnings("unchecked")
		Command<Integer> square = context.resolve(
				raw(Command.class).parameterized(Integer.class));
		assertNotNull(context.resolve(MathService.class));
		assertEquals(9L, square.calc(3));
	}
}
