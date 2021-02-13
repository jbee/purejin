package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Hint;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.lang.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;

/**
 * Test illustrates how to inject specific implementations for a parameter that
 * uses an interface for which mupltiple implementations and/or instances
 * exist.
 * <p>
 * The two concept usually used to solve such situations are {@link
 * se.jbee.inject.binder.Binder.ScopedBinder#injectingInto(Type)} or using
 * {@link Hint}s which are passed to {@link se.jbee.inject.binder.Binder.TypedBinder#toConstructor(Hint[])}.
 * <p>
 * The difference between the two is that {@link Hint} add detail information to
 * the binding created so that it knows what to resolve for particular arguments
 * while {@code injectingInto} creates a binding that is localised to a specific
 * target instance and which takes precedence should there be another general
 * binding.
 */
class TestExampleHandPickedInjectionBinds {

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

	static class TestExampleHandPickedInjectionBindsModule
			extends BinderModule {

		static final Instance<GenericAction> b = instance(named("b"),
				raw(GenericAction.class));
		static final Instance<GenericAction> c = instance(named("c"),
				raw(GenericAction.class));

		@Override
		protected void declare() {
			// using hints
			bind(Receiver.class).toConstructor(
					Hint.relativeReferenceTo(ActionA.class), b.asHint(),
					c.asHint());
			// using injectingInto
			injectingInto(c).bind(String.class).to("and this is c");
			// general construction
			bind(ActionA.class).toConstructor();
			bind(b).to(new GenericAction("this is b"));
			bind(c).toConstructor();
		}
	}

	private final Injector context = Bootstrap.injector(
			TestExampleHandPickedInjectionBindsModule.class);

	@Test
	void implementationIsPickedAsSpecified() {
		Receiver r = context.resolve(Receiver.class);
		assertEquals(ActionA.class, r.a.getClass());
		assertEquals(GenericAction.class, r.b.getClass());
		assertEquals(GenericAction.class, r.c.getClass());
		assertEquals("this is b", r.b.doIt());
		assertEquals("and this is c", r.c.doIt());
	}
}
