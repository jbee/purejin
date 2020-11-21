package test.integration.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.action.ActionModule.actionDependency;

import org.junit.jupiter.api.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.Supplier;
import se.jbee.inject.action.Action;
import se.jbee.inject.action.ActionModule;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * This test demonstrates that it is possible to have different higher level
 * 'service' on top of {@link Action}s.
 *
 * While the {@link TestServiceBinds} shows how do build a generic service this
 * test shows a simpler version {@link Command} of such generic service having a
 * fix return type. Thereby it is very well possible to use different higher
 * level services in the same time.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestCommandBinds {

	private interface Command<P> {

		Long calc(P param);
	}

	private static class CommandSupplier implements Supplier<Command<?>> {

		@Override
		public Command<?> supply(Dependency<? super Command<?>> dep,
				Injector context) {
			return newCommand(context.resolve(actionDependency(
					dep.type().parameter(0), raw(Long.class))));
		}

		private static <P> Command<P> newCommand(Action<P, Long> service) {
			return new CommandToServiceMethodAdapter<>(service);
		}

		static class CommandToServiceMethodAdapter<P> implements Command<P> {

			private final Action<P, Long> service;

			CommandToServiceMethodAdapter(Action<P, Long> service) {
				this.service = service;
			}

			@Override
			public Long calc(P param) {
				return service.run(param);
			}

		}
	}

	private static class CommandBindsModule extends ActionModule {

		@Override
		protected void declare() {
			bindActionsIn(MathService.class);
			per(Scope.dependencyType) //
					.starbind(Command.class) //
					.toSupplier(new CommandSupplier());
		}

	}

	public static class MathService {

		public Long square(Integer value) {
			return value.longValue() * value;
		}
	}

	@Test
	public void thatServiceCanBeResolvedWhenHavingJustOneGeneric() {
		Injector injector = Bootstrap.injector(CommandBindsModule.class);
		@SuppressWarnings("unchecked")
		Command<Integer> square = injector.resolve(
				raw(Command.class).parametized(Integer.class));
		assertEquals(9L, square.calc(3).longValue());
	}
}
