package de.jbee.inject;

import static de.jbee.inject.Dependency.dependency;
import static de.jbee.inject.Resource.resource;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.jbee.inject.util.Scoped;

public class TestScopes {

	private static class ConstantInjectable<T>
			implements Injectable<T> {

		private final T instance;

		ConstantInjectable( T instance ) {
			super();
			this.instance = instance;
		}

		@Override
		public T instanceFor( Demand<T> demand ) {
			return instance;
		}
	}

	private static class A {
		// just for test
	}

	private static class B {
		// just for test
	}

	@Test
	public void thatDependencyTypeScopeEnsuresSingletonPerExactGenericType() {
		Repository r = Scoped.DEPENDENCY_TYPE.init();
		Demand<A> da = new Demand<A>( resource( A.class ), dependency( A.class ), 1, 2 );
		Demand<B> db = new Demand<B>( resource( B.class ), dependency( B.class ), 2, 2 );
		A a = new A();
		B b = new B();
		Injectable<A> ia = new ConstantInjectable<A>( a );
		Injectable<B> ib = new ConstantInjectable<B>( b );
		assertThat( r.serve( da, ia ), sameInstance( a ) );
		assertThat( r.serve( da, null ), sameInstance( a ) ); // the null Injectable shouldn't be called now 
		assertThat( r.serve( db, ib ), sameInstance( b ) );
		assertThat( r.serve( db, null ), sameInstance( b ) ); // the null Injectable shouldn't be called now
	}
}
