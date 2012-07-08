package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;

public class TestTargetedBinds {

	static final MyBar BAR_IN_FOO = new MyBar();
	static final MyBar BAR_EVERYWHERE_ELSE = new MyBar();

	private static class TargetedBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( MyFoo.class ).to( MyFoo.class );
			injectingInto( MyFoo.class ).bind( MyBar.class ).to( BAR_IN_FOO );
			bind( MyBar.class ).to( BAR_EVERYWHERE_ELSE );
		}
	}

	private static class MyFoo {

		final MyBar bar;

		@SuppressWarnings ( "unused" )
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
	public void thatBindWithTargetIsUsedWhenInjectingIntoIt() {
		DependencyResolver injector = Bootstrap.injector( TargetedBindsModule.class );
		MyFoo foo = injector.resolve( dependency( MyFoo.class ) );
		assertThat( foo.bar, sameInstance( BAR_IN_FOO ) );
	}

	@Test
	public void thatBindWithTargetIsNotUsedWhenNotInjectingIntoIt() {
		DependencyResolver injector = Bootstrap.injector( TargetedBindsModule.class );
		MyBar bar = injector.resolve( dependency( MyBar.class ) );
		assertThat( bar, sameInstance( BAR_EVERYWHERE_ELSE ) );
	}
}
