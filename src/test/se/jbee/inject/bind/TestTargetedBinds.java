package se.jbee.inject.bind;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.io.Serializable;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.bind.BasicBinder.ScopedBasicBinder;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * A test that demonstrates how to inject a specific instance into another type using the
 * {@link ScopedBasicBinder#injectingInto(se.jbee.inject.Instance)} method.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestTargetedBinds {

	/**
	 * We use different {@link Bar} constants to check if the different {@link Foo}s got their
	 * desired {@linkplain Bar}.
	 */
	static final Bar BAR_IN_FOO = new Bar();
	static final Bar BAR_EVERYWHERE_ELSE = new Bar();
	static final Bar BAR_IN_AWESOME_FOO = new Bar();
	static final Bar BAR_IN_SERIALIZABLE = new Bar();
	static final Bar BAR_IN_QUX = new Bar();

	private static class TargetedBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			construct( Foo.class );
			injectingInto( Foo.class ).bind( Bar.class ).to( BAR_IN_FOO );
			bind( Bar.class ).to( BAR_EVERYWHERE_ELSE );
			Name special = named( "special" );
			construct( special, Foo.class ); // if we would use a type bind like to(Foo.class) it wouldn't work since we use a Foo that is not created as special Foo so it got the other Bar 
			injectingInto( special, Foo.class ).bind( Bar.class ).to( BAR_EVERYWHERE_ELSE );
			Name awesome = named( "awesome" );
			construct( awesome, Foo.class );
			injectingInto( awesome, Foo.class ).bind( Bar.class ).to( BAR_IN_AWESOME_FOO );
			construct( Baz.class );
			TargetedBinder binder = injectingInto( Serializable.class );
			binder.bind( Bar.class ).to( BAR_IN_SERIALIZABLE );
			construct( Qux.class );
			injectingInto( Qux.class ).bind( Bar.class ).to( BAR_IN_QUX );
		}
	}

	private static class Foo {

		final Bar bar;

		@SuppressWarnings ( "unused" )
		Foo( Bar bar ) {
			this.bar = bar;
		}

	}

	private static class Bar {

		Bar() {
			// make visible
		}
	}

	private static class Baz
			implements Serializable {

		final Bar bar;

		@SuppressWarnings ( "unused" )
		Baz( Bar bar ) {
			this.bar = bar;
		}
	}

	private static class Qux
			implements Serializable {

		final Bar bar;

		@SuppressWarnings ( "unused" )
		Qux( Bar bar ) {
			this.bar = bar;
		}
	}

	private final Injector injector = Bootstrap.injector( TargetedBindsModule.class );

	@Test
	public void thatBindWithTargetIsUsedWhenInjectingIntoIt() {
		Foo foo = injector.resolve( dependency( Foo.class ) );
		assertThat( foo.bar, sameInstance( BAR_IN_FOO ) );
	}

	@Test
	public void thatBindWithTargetIsNotUsedWhenNotInjectingIntoIt() {
		Bar bar = injector.resolve( dependency( Bar.class ) );
		assertThat( bar, sameInstance( BAR_EVERYWHERE_ELSE ) );
	}

	@Test
	public void thatNamedTargetIsUsedWhenInjectingIntoIt() {
		Instance<Foo> specialFoo = instance( named( "special" ), raw( Foo.class ) );
		Bar bar = injector.resolve( dependency( Bar.class ).injectingInto( specialFoo ) );
		assertThat( bar, sameInstance( BAR_EVERYWHERE_ELSE ) );
	}

	@Test
	public void thatBindWithNamedTargetIsUsedWhenInjectingIntoIt() {
		Dependency<Foo> fooDependency = dependency( Foo.class );
		Foo foo = injector.resolve( fooDependency.named( named( "special" ) ) );
		assertThat( foo.bar, sameInstance( BAR_EVERYWHERE_ELSE ) );
		foo = injector.resolve( fooDependency.named( named( "Awesome" ) ) );
		assertThat( foo.bar, sameInstance( BAR_IN_AWESOME_FOO ) );
	}

	@Test
	public void thatBindWithInterfaceTargetIsUsedWhenInjectingIntoClassHavingThatInterface() {
		Baz baz = injector.resolve( dependency( Baz.class ) );
		assertThat( baz.bar, sameInstance( BAR_IN_SERIALIZABLE ) );
	}

	@Test
	public void thatBindWithExactClassTargetIsUsedWhenInjectingIntoClassHavingThatClassButAlsoAnInterfaceMatching() {
		Qux qux = injector.resolve( dependency( Qux.class ) );
		assertThat( qux.bar, sameInstance( BAR_IN_QUX ) );
	}
}
