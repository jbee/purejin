package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;

public class TestTargetedBinds {

	static final MyBar BAR0 = new MyBar();
	static final MyBar BAR1 = new MyBar();

	private static class TargetedBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( MyFoo.class ).to( MyFoo.class );
			bind( MyBar.class ).to( BAR0 );
			injectingInto( MyFoo.class ).bind( MyBar.class ).to( BAR1 );
		}
	}

	private static class MyFoo {

		final MyBar bar;

		MyFoo( MyBar bar ) {
			super();
			this.bar = bar;
		}

	}

	private static class MyBar {

		MyBar() {
			// make visible
		}
	}

	@Test
	public void thatBindWithTargetIsUsedWhenInjectingBarIntoFoo() {
		DependencyResolver injector = Bootstrap.injector( TargetedBindsModule.class );
		MyFoo foo = injector.resolve( dependency( MyFoo.class ) );
		assertThat( foo.bar, sameInstance( BAR1 ) );
	}

	@Test
	public void thatBindWithTargetIsNotUsedWhenNotInjectingBarIntoFoo() {
		DependencyResolver injector = Bootstrap.injector( TargetedBindsModule.class );
		assertThat( injector.resolve( dependency( MyBar.class ) ), sameInstance( BAR0 ) );
	}
}
