package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static se.jbee.inject.Name.named;
import static test.integration.util.TestUtils.assertEqualSets;

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

	static final Name CMD_1 = named("command1");
	static final Name CMD_2 = named("command2");
	static final Name CMD_3 = named("command3");
	static final Name CMD_4 = named("command4");
	static final Name CMD_5 = named("command5");
	static final Name CMD_6 = named("command6");

	static final Name PRE_2 = named("precond2");

	static class TestBasicArrayBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(PRE_2, Double.class).to(2.0d); // used by both CMD_1 and CMD_2

			bind(CMD_1, Command.class).toConstructor();
			injectingInto(CMD_1, Command.class).multibind(Number.class).to(1);
			injectingInto(CMD_1, Command.class).multibind(Number.class).to(
					PRE_2, Double.class);

			bind(CMD_2, Command.class).toConstructor();
			injectingInto(CMD_2, Command.class).multibind(Number.class).to(
					PRE_2, Double.class);
			injectingInto(CMD_2, Command.class).multibind(Number.class).to(3f);
			injectingInto(CMD_2, Command.class).multibind(Number.class).to(5L);

			bind(CMD_3, Command.class).toConstructor();
			injectingInto(CMD_3, Command.class).bind(Number.class).toMany(1, 6d, 8);

			bind(CMD_4, Command.class).toConstructor(
					Hint.constant(new Number[] { 2d, 9 }));

			bind(CMD_5, Command.class).toConstructor();
			injectingInto(CMD_5, Command.class).arraybind(
					Number[].class).toElements(1, 2, 3);

			bind(CMD_6, Command.class).toConstructor();
			injectingInto(CMD_6, Command.class).bind(Number[].class).to(
					new Number[] { 4, 5, 6 });
		}
	}

	private final Injector injector = Bootstrap.injector(
			TestBasicArrayBindsModule.class);

	@Test
	void thatEachCommandGetsOnlyThePreconditionsBoundToIt() {
		assertPreconditions(CMD_1, 1, 2.0d);
		assertPreconditions(CMD_2, 2d, 3f, 5L);
		assertPreconditions(CMD_3, 1, 6d, 8);
		assertPreconditions(CMD_4, 2d, 9);
		assertPreconditions(CMD_5, 1, 2, 3);
		assertPreconditions(CMD_6, 4, 5, 6);
	}

	private void assertPreconditions(Name command, Number... expected) {
		assertEqualSets(expected,
				injector.resolve(command, Command.class).preconds);
	}
}
