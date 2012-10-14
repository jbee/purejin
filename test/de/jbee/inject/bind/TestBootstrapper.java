package de.jbee.inject.bind;

import static de.jbee.inject.Type.raw;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import de.jbee.inject.Dependency;
import de.jbee.inject.Injector;
import de.jbee.inject.Name;
import de.jbee.inject.Resource;
import de.jbee.inject.DIRuntimeException.DependencyCycleException;

/**
 * The tests shows an example of cyclic depended {@link Bundle}s. It shows that a {@link Bundle}
 * doesn't have to know or consider other bundles since it is valid to make cyclic references or
 * install the {@link Bundle}s multiple times.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public class TestBootstrapper {

	/**
	 * One of two bundles in a minimal example of mutual dependent bundles. While this installs
	 * {@link OtherMutualDependentBundle} that bundle itself installs this bundle. This should not
	 * be a problem and both bundles are just installed once.
	 */
	private static class OneMutualDependentBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( OtherMutualDependentBundle.class );
		}

	}

	private static class OtherMutualDependentBundle
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( OneMutualDependentBundle.class );
		}

	}

	/**
	 * Because the same {@link Resource} is defined twice (the {@link Name#DEFAULT} {@link Integer}
	 * instance) this module should cause an exception. All {@link Resource} have to be unique.
	 */
	private static class ClashingBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Integer.class ).to( 42 );
			bind( Integer.class ).to( 8 );
		}

	}

	@SuppressWarnings ( "unused" )
	private static class Foo {

		Foo( Bar bar ) {
			// something
		}
	}

	@SuppressWarnings ( "unused" )
	private static class Bar {

		public Bar( Foo foo ) {
			// something
		}
	}

	private static class A {

		A( B b ) {

		}
	}

	private static class B {

		B( C c ) {
		}
	}

	private static class C {

		C( A a ) {
		}
	}

	private static class CyclicBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( Foo.class ).toConstructor( raw( Bar.class ) );
			bind( Bar.class ).toConstructor( raw( Foo.class ) );
		}

	}

	private static class CircleBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			bind( A.class ).toConstructor( raw( B.class ) );
			bind( B.class ).toConstructor( raw( C.class ) );
			bind( C.class ).toConstructor( raw( A.class ) );
		}

	}

	/**
	 * The assert itself doesn't play such huge role here. we just want to reach this code.
	 */
	@Test
	public void thatBundlesAreNotBootstrappedMultipleTimesEvenWhenTheyAreMutual() {
		Injector injector = Bootstrap.injector( OneMutualDependentBundle.class );
		assertThat( injector, notNullValue() );
	}

	@Test ( expected = IllegalStateException.class )
	public void thatNonUniqueResourcesThrowAnException() {
		Bootstrap.injector( ClashingBindsModule.class );
	}

	@Test ( expected = DependencyCycleException.class )
	public void thatDependencyCyclesAreDetected() {
		Injector injector = Bootstrap.injector( CyclicBindsModule.class );
		Foo foo = injector.resolve( Dependency.dependency( Foo.class ) );
		fail( "foo should not be resolvable but was: " + foo );
	}

	@Test ( expected = DependencyCycleException.class )
	public void thatDependencyCyclesInCirclesAreDetected() {
		Injector injector = Bootstrap.injector( CircleBindsModule.class );
		A a = injector.resolve( Dependency.dependency( A.class ) );
		fail( "A should not be resolvable but was: " + a );
	}

}
