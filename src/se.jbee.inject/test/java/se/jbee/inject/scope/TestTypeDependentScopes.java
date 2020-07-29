package se.jbee.inject.scope;

import static org.junit.Assert.assertSame;
import static se.jbee.inject.Dependency.dependency;

import org.junit.Test;

import se.jbee.inject.Provider;
import se.jbee.inject.Scope;

public class TestTypeDependentScopes {

	static class A {
		// just for test
	}

	static class B {
		// just for test
	}

	@Test
	public void thatDependencyTypeScopeEnsuresSingletonPerExactGenericType() {
		Scope scope = new DependencyScope(DependencyScope::typeName);
		A a = new A();
		B b = new B();
		Provider<A> ia = () -> a;
		Provider<B> ib = () -> b;
		assertSame(scope.provide(1, dependency(A.class), ia, 2), a);
		assertSame(scope.provide(1, dependency(A.class), null, 2), a); // the null Provider shouldn't be called now
		assertSame(scope.provide(2, dependency(B.class), ib, 2), b);
		assertSame(scope.provide(2, dependency(B.class), null, 2), b); // the null Provider shouldn't be called now
	}

}
