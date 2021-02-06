package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static se.jbee.inject.Name.named;
import static se.jbee.junit.assertion.Assertions.assertEqualsIgnoreOrder;

/**
 * How to inject different arrays into different instances of the same parent
 * type.
 */
class TestBasicArrayBinds {

	public static class Command {

		final Number[] preconds;

		public Command(Number[] preconds) {
			this.preconds = preconds;
		}
	}

	static final Name PRE_2 = named("precond2");

	static class TestBasicArrayBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(PRE_2, Double.class).to(2.0d); // used by both CMD_1 and CMD_2

			bind("command1", Command.class).toConstructor();
			injectingInto("command1", Command.class).multibind(Number.class).to(1);
			injectingInto("command1", Command.class).multibind(Number.class).to(
					PRE_2, Double.class);

			bind("command2", Command.class).toConstructor();
			injectingInto("command2", Command.class).multibind(Number.class).to(
					PRE_2, Double.class);
			injectingInto("command2", Command.class).multibind(Number.class).to(3f);
			injectingInto("command2", Command.class).multibind(Number.class).to(5L);

			bind("command3", Command.class).toConstructor();
			injectingInto("command3", Command.class).bind(Number.class).toMultiple(1, 6d, 8);

			bind("command4", Command.class).toConstructor(
					Hint.constant(new Number[] { 2d, 9 }));

			bind("command5", Command.class).toConstructor();
			injectingInto("command5", Command.class).arraybind(
					Number[].class).toElements(1, 2, 3);

			bind("command6", Command.class).toConstructor();
			injectingInto("command6", Command.class).bind(Number[].class).to(
					new Number[] { 4, 5, 6 });
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestBasicArrayBindsModule.class);

	@Test
	void thatEachCommandGetsOnlyThePreconditionsBoundToIt() {
		assertPreconditions("command1", 1, 2.0d);
		assertPreconditions("command2", 2d, 3f, 5L);
		assertPreconditions("command3", 1, 6d, 8);
		assertPreconditions("command4", 2d, 9);
		assertPreconditions("command5", 1, 2, 3);
		assertPreconditions("command6", 4, 5, 6);
	}

	private void assertPreconditions(String command, Number... expected) {
		assertEqualsIgnoreOrder(expected,
				injector.resolve(command, Command.class).preconds);
	}
}
