package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.scope.TypeDependentScope;

import static org.junit.jupiter.api.Assertions.assertSame;
import static se.jbee.inject.Dependency.dependency;

/**
 * A basic test verifying the {@link TypeDependentScope} implementation behaves
 * as expected.
 */
class TestFeatureTypeDependentScopes {

	static class A {
		// just for test
	}

	static class B {
		// just for test
	}

	@Test
	void dependencyTypeScopeEnsuresSingletonPerExactGenericType() {
		Scope scope = TypeDependentScope.perTypeSignature();
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
