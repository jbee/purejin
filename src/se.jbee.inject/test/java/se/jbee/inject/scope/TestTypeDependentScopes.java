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
		Scope scope = new TypeDependentScope(TypeDependentScope::typeSignature);
		A a = new A();
		B b = new B();
		Provider<A> ia = () -> a;
		Provider<B> ib = () -> b;
		assertSame(scope.provide(1, 2, dependency(A.class), ia), a);
		assertSame(scope.provide(1, 2, dependency(A.class), null), a); // the null Provider shouldn't be called now
		assertSame(scope.provide(2, 2, dependency(B.class), ib), b);
		assertSame(scope.provide(2, 2, dependency(B.class), null), b); // the null Provider shouldn't be called now
	}

}
