package de.jbee.inject.bind;

import static de.jbee.inject.Dependency.dependency;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.DependencyResolver;
import de.jbee.inject.bind.BasicBinder.ScopedBasicBinder;

/**
 * A test that demonstrates how to inject a specific instance into another type using the
 * {@link ScopedBasicBinder#injectingInto(de.jbee.inject.Instance)} method.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public class TestTargetedBinds {

	static final Bar BAR_IN_FOO = new Bar();
	static final Bar BAR_EVERYWHERE_ELSE = new Bar();

	private static class TargetedBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Foo.class ).to( Foo.class );
			injectingInto( Foo.class ).bind( Bar.class ).to( BAR_IN_FOO );
			bind( Bar.class ).to( BAR_EVERYWHERE_ELSE );
		}
	}

	private static class Foo {

		final Bar bar;

		@SuppressWarnings ( "unused" )
		Foo( Bar bar ) {
			super();
			this.bar = bar;
		}

	}

	private static class Bar {

		Bar() {
			// make visible
		}
	}

	@Test
	public void thatBindWithTargetIsUsedWhenInjectingIntoIt() {
		DependencyResolver injector = Bootstrap.injector( TargetedBindsModule.class );
		Foo foo = injector.resolve( dependency( Foo.class ) );
		assertThat( foo.bar, sameInstance( BAR_IN_FOO ) );
	}

	@Test
	public void thatBindWithTargetIsNotUsedWhenNotInjectingIntoIt() {
		DependencyResolver injector = Bootstrap.injector( TargetedBindsModule.class );
		Bar bar = injector.resolve( dependency( Bar.class ) );
		assertThat( bar, sameInstance( BAR_EVERYWHERE_ELSE ) );
	}
}
