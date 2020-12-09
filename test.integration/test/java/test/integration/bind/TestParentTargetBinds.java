package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestParentTargetBinds {

	private static class ParentTargetBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("everywhere");
			per(Scope.targetInstance).construct(Child.class);
			injectingInto(Child.class).bind(String.class).to("child");
			per(Scope.targetInstance).construct(Parent.class);
			injectingInto(Child.class).within(Parent.class) //
					.bind(String.class).to("child-with-parent");
			construct(Grandparent.class);
			injectingInto(Child.class).within(Parent.class).within(
					Grandparent.class) //
					.bind(String.class).to("child-with-grandparent");
		}
	}

	public static class Grandparent {

		final Parent parent;

		public Grandparent(Parent parent) {
			this.parent = parent;
		}

	}

	public static class Parent {

		final Child child;

		public Parent(Child child) {
			this.child = child;
		}

	}

	public static class Child {

		final String value;

		public Child(String value) {
			this.value = value;
		}

	}

	private final Injector injector = Bootstrap.injector(
			ParentTargetBindsModule.class);

	@Test
	public void thatBindWithParentTargetIsUsedWhenInjectionHasParent() {
		assertEquals("child-with-grandparent",
				injector.resolve(Grandparent.class).parent.child.value);
		assertEquals("child-with-parent",
				injector.resolve(Parent.class).child.value);
		assertEquals("child", injector.resolve(Child.class).value);
	}

	/**
	 * counter-check to see that the more precise bind does not apply here
	 */
	@Test
	public void thatBindWithParentTargetIsNotUsedWhenInjectionHasNoParent() {
		assertEquals("child", injector.resolve(Child.class).value);
		assertEquals("everywhere", injector.resolve(String.class));
	}

}
