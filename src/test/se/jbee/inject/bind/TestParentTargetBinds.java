package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static se.jbee.inject.Dependency.dependency;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.util.Scoped;

public class TestParentTargetBinds {

	private static class ParentTargetBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( String.class ).to( "everywhere" );
			per( Scoped.TARGET_INSTANCE ).construct( Child.class );
			injectingInto( Child.class ).bind( String.class ).to( "child" );
			per( Scoped.TARGET_INSTANCE ).construct( Parent.class );
			injectingInto( Child.class ).within( Parent.class ) //
			.bind( String.class ).to( "child-with-parent" );
			construct( Grandparent.class );
			injectingInto( Child.class ).within( Parent.class ).within( Grandparent.class ) //
			.bind( String.class ).to( "child-with-grandparent" );
		}
	}

	private static class Grandparent {

		final Parent parent;

		private Grandparent( Parent parent ) {
			super();
			this.parent = parent;
		}

	}

	private static class Parent {

		final Child child;

		private Parent( Child child ) {
			super();
			this.child = child;
		}

	}

	private static class Child {

		final String value;

		private Child( String value ) {
			super();
			this.value = value;
		}

	}

	private final Injector injector = Bootstrap.injector( ParentTargetBindsModule.class );

	@Test
	public void thatBindWithParentTargetIsUsedWhenInjectionHasParent() {
		Grandparent grandparent = injector.resolve( dependency( Grandparent.class ) );
		assertEquals( "child-with-grandparent", grandparent.parent.child.value );
		Parent parent = injector.resolve( dependency( Parent.class ) );
		assertEquals( "child-with-parent", parent.child.value );
		assertEquals( "child", injector.resolve( dependency( Child.class ) ).value );
	}

	/**
	 * counter-check to see that the more precise bind does not apply here
	 */
	@Test
	public void thatBindWithParentTargetIsNotUsedWhenInjectionHasNoParent() {
		assertEquals( "child", injector.resolve( dependency( Child.class ) ).value );
		assertEquals( "everywhere", injector.resolve( dependency( String.class ) ) );
	}

}
