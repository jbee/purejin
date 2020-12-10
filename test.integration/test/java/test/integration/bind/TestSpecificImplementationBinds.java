package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.raw;

/**
 * Test illustrates how to inject specific implementation for same interface
 * into the same receiver class.
 */
class TestSpecificImplementationBinds {

	interface Action {
		String doIt();
	}

	public static class Receiver {

		final Action a;
		final Action b;
		final Action c;

		public Receiver(Action a, Action b, Action c) {
			this.a = a;
			this.b = b;
			this.c = c;
		}
	}

	public static class ActionA implements Action {

		@Override
		public String doIt() {
			return "whatever";
		}

	}

	public static class GenericAction implements Action {

		public final String state;

		public GenericAction(String state) {
			this.state = state;
		}

		@Override
		public String doIt() {
			return state;
		}

	}

	static class SpecificImplementationBindsModule extends BinderModule {

		@Override
		protected void declare() {
			Instance<GenericAction> b = instance(named("b"),
					raw(GenericAction.class));
			Instance<GenericAction> c = instance(named("c"),
					raw(GenericAction.class));
			bind(Receiver.class).toConstructor(
					Hint.relativeReferenceTo(ActionA.class), b.asHint(), c.asHint());
			bind(ActionA.class).toConstructor();
			bind(b).to(new GenericAction("this is b"));
			bind(c).toConstructor();
			injectingInto(c).bind(String.class).to("and this is c");
		}

	}

	@Test
	void thatImplementationIsPickedAsSpecified() {
		Injector injector = Bootstrap.injector(
				SpecificImplementationBindsModule.class);

		Receiver r = injector.resolve(Receiver.class);
		assertEquals(ActionA.class, r.a.getClass());
		assertEquals(GenericAction.class, r.b.getClass());
		assertEquals(GenericAction.class, r.c.getClass());
		assertEquals("this is b", r.b.doIt());
		assertEquals("and this is c", r.c.doIt());
	}
}
