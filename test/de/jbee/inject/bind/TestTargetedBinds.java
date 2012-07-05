package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.sameInstance;

import org.junit.Assert;
import org.junit.Test;

import de.jbee.inject.DependencyResolver;

public class TestTargetedBinds {

	static final MyBar BAR1 = new MyBar();

	private static class TargetedBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( MyFoo.class ).to( MyFoo.class );
			injectingInto( MyFoo.class ).bind( MyBar.class ).to( BAR1 );
		}
	}

	private static class MyFoo {

		final MyBar bar;

		private MyFoo( MyBar bar ) {
			super();
			this.bar = bar;
		}

	}

	private static class MyBar {

	}

	@Test
	public void test() {
		DependencyResolver injector = Bootstrap.injector( TargetedBindsModule.class );
		MyFoo foo = injector.resolve( dependency( MyFoo.class ) );
		Assert.assertThat( foo.bar, sameInstance( BAR1 ) );
	}
}
