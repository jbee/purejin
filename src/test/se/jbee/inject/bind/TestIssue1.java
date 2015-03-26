package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Type.raw;
import static se.jbee.inject.bind.AssertInjects.assertEqualSets;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.BootstrapperBundle;

/**
 * Solution for cycle on common interface injecting other implementations into one of them.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestIssue1 {

	static interface A {

	}

	static class B
			implements A {

		final A[] as;

		B( A[] as ) {
			this.as = as;
		}
	}

	static class C
			implements A {

	}

	static class D
			implements A {

	}

	static Name left = Name.named( "left" );
	static Name right = Name.named( "right" );
	static Name special = Name.named( "special" );

	static class Module1
			extends BinderModule {

		@Override
		protected void declare() {
			bind( left, A.class ).to( B.class );
			bind( right, A.class ).to( left, B.class );
			bind( left, B.class ).toConstructor();
			bind( B.class ).toConstructor();
			injectingInto( left, B.class ).bind( A[].class ).to( special, A[].class );
			arraybind( A[].class ).to( new A[0] );
		}
	}

	static class Module2
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( special, A.class ).to( C.class );
		}
	}

	static class Module3
			extends BinderModule {

		@Override
		protected void declare() {
			multibind( special, A.class ).to( D.class );
		}

	}

	static class Bundle1
			extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install( Module1.class );
			install( Module2.class );
			install( Module3.class );
		}

	}

	@Test
	public void thatBundleCanBeBootstrapped() {
		Injector injector = Bootstrap.injector( Bundle1.class );
		B b = injector.resolve( dependency( B.class ) );
		B leftB = injector.resolve( dependency( B.class ).named( left ) );
		assertNotSame( b, leftB );
		assertEquals( 2, leftB.as.length );
		C c = injector.resolve( dependency( C.class ).injectingInto(
				instance( left, raw( B.class ) ) ) );
		D d = injector.resolve( dependency( D.class ).injectingInto(
				instance( left, raw( B.class ) ) ) );
		assertEqualSets( new A[] { c, d }, leftB.as );
	}
}
