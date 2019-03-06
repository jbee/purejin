package se.jbee.inject.bind;

import static se.jbee.inject.bind.AssertInjects.assertEqualSets;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;
import se.jbee.inject.bootstrap.ChoiceBootstrapperBundle;
import se.jbee.inject.config.Choices;
import se.jbee.inject.config.Globals;

public class TestMultipleChoicesBinds {

	private enum Text {
		A, B, C, D, E
	}

	private static class A extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("A");
		}
	}

	private static class B extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("B");
		}
	}

	private static class C extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("C");
		}
	}

	private static class D extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("D");
		}
	}

	private static class E extends BinderModule {

		@Override
		protected void declare() {
			multibind(String.class).to("E");
		}
	}

	private static class ChoicesBundle extends ChoiceBootstrapperBundle<Text> {

		@Override
		protected void bootstrap() {
			install(A.class, Text.A);
			install(B.class, Text.B);
			install(C.class, Text.C);
			install(D.class, Text.D);
			install(E.class, Text.E);
		}
	}

	private static class RootBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(ChoicesBundle.class, Text.class);
		}
	}

	@Test
	public void thatMultipleChoicesArePossible() {
		Choices choices = Choices.STANDARD.chooseMultiple(Text.A, Text.D);
		Globals globals = Globals.STANDARD.with(choices);
		Injector injector = Bootstrap.injector(RootBundle.class, globals);
		assertEqualSets(new String[] { "A", "D" },
				injector.resolve(String[].class));
	}
}
