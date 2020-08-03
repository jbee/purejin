package se.jbee.inject.bootstrap;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

public class TestParentTargetBinds {

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

	private static class Grandparent {

		final Parent parent;

		private Grandparent(Parent parent) {
			this.parent = parent;
		}

	}

	private static class Parent {

		final Child child;

		private Parent(Child child) {
			this.child = child;
		}

	}

	private static class Child {

		final String value;

		private Child(String value) {
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
