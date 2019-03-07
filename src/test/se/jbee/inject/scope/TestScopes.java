package se.jbee.inject.scope;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Scoping.scopingOf;

import org.junit.Test;

import se.jbee.inject.Name;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.Scoping;

public class TestScopes {

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
		assertSame(scope.yield(1, dependency(A.class), ia, 2), a);
		assertSame(scope.yield(1, dependency(A.class), null, 2), a); // the null Provider shouldn't be called now
		assertSame(scope.yield(2, dependency(B.class), ib, 2), b);
		assertSame(scope.yield(2, dependency(B.class), null, 2), b); // the null Provider shouldn't be called now
	}

	@Test
	public void injectionScopeIsNotStableInAnyOtherScope() {
		Scoping injection = scopingOf(Scope.injection);
		assertFalse(injection.isStableIn(Scope.application));
		assertFalse(injection.isStableIn(Scope.dependency));
		assertFalse(injection.isStableIn(Scope.dependencyInstance));
		assertFalse(injection.isStableIn(Scope.dependencyType));
		assertFalse(injection.isStableIn(Scope.targetInstance));
		assertTrue(injection.isStableIn(injection));
	}

	@Test
	public void threadScopeIsNotStableInAnyOtherScopeExceptInjection() {
		Scoping thread = scopingOf(Scope.thread);
		assertFalse(thread.isStableIn(Scope.application));
		assertFalse(thread.isStableIn(Scope.dependency));
		assertFalse(thread.isStableIn(Scope.dependencyInstance));
		assertFalse(thread.isStableIn(Scope.dependencyType));
		assertFalse(thread.isStableIn(Scope.targetInstance));
		assertTrue(thread.isStableIn(thread));
		assertTrue(thread.isStableIn(Scope.injection));
	}

	@Test
	public void applicationIsStableInAnyOtherScope() {
		assertStableScope(Scope.application);
	}

	@Test
	public void dependencyBasedIsStableInAnyOtherScope() {
		assertStableScope(Scope.dependency);
		assertStableScope(Scope.dependencyInstance);
		assertStableScope(Scope.dependencyType);
		assertStableScope(Scope.targetInstance);
	}

	@Test
	public void ignoreIsStableInAnyOtherScope() {
		assertStableScope(Scoping.ignore);
	}

	private static void assertStableScope(Name scope) {
		assertStableScope(scopingOf(scope));
	}

	private static void assertStableScope(Scoping s) {
		assertTrue(s.isStableIn(s.scope));
		assertTrue(s.isStableIn(s));
		assertTrue(s.isStableIn(Scope.dependency));
		assertTrue(s.isStableIn(Scope.dependencyInstance));
		assertTrue(s.isStableIn(Scope.dependencyType));
		assertTrue(s.isStableIn(Scope.targetInstance));
		assertTrue(s.isStableIn(Scope.thread));
		assertTrue(s.isStableIn(Scope.injection));
	}

}
