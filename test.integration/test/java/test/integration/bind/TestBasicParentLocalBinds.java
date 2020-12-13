package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestBasicParentLocalBinds {

	private static class TestBasicParentLocalBindsModule extends BinderModule {

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

	private final Injector context = Bootstrap.injector(
			TestBasicParentLocalBindsModule.class);

	@Test
	void bindWithParentTargetIsUsedWhenInjectionHasParent() {
		assertEquals("child-with-grandparent",
				context.resolve(Grandparent.class).parent.child.value);
		assertEquals("child-with-parent",
				context.resolve(Parent.class).child.value);
		assertEquals("child", context.resolve(Child.class).value);
	}

	/**
	 * counter-check to see that the more precise bind does not apply here
	 */
	@Test
	void bindWithParentTargetIsNotUsedWhenInjectionHasNoParent() {
		assertEquals("child", context.resolve(Child.class).value);
		assertEquals("everywhere", context.resolve(String.class));
	}
}
