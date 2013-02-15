package se.jbee.inject.util;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static se.jbee.inject.Demand.demand;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Resource.resource;

import org.junit.Test;

import se.jbee.inject.Demand;
import se.jbee.inject.Injectable;
import se.jbee.inject.Repository;
import se.jbee.inject.util.Scoped;

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

	static class A {
		// just for test
	}

	static class B {
		// just for test
	}

	@Test
	public void thatDependencyTypeScopeEnsuresSingletonPerExactGenericType() {
		Repository r = Scoped.DEPENDENCY_TYPE.init();
		Demand<A> da = demand( resource( A.class ), dependency( A.class ), 1, 2 );
		Demand<B> db = demand( resource( B.class ), dependency( B.class ), 2, 2 );
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
