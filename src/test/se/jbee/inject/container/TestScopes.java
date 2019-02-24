package se.jbee.inject.container;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Scoping.scopingOf;

import org.junit.Test;

import se.jbee.inject.Provider;
import se.jbee.inject.Repository;
import se.jbee.inject.Scope;
import se.jbee.inject.Scoping;

public class TestScopes {

	private static class ConstantProvider<T> implements Provider<T> {

		private final T instance;

		ConstantProvider(T instance) {
			this.instance = instance;
		}

		@Override
		public T provide() {
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
		Repository r = Scoped.DEPENDENCY_TYPE.init(2);
		A a = new A();
		B b = new B();
		Provider<A> ia = new ConstantProvider<>(a);
		Provider<B> ib = new ConstantProvider<>(b);
		assertSame(r.serve(1, dependency(A.class), ia), a);
		assertSame(r.serve(1, dependency(A.class), null), a); // the null Provider shouldn't be called now
		assertSame(r.serve(2, dependency(B.class), ib), b);
		assertSame(r.serve(2, dependency(B.class), null), b); // the null Provider shouldn't be called now
	}

	@Test
	public void injectionScopeIsNotStableInAnyOtherScope() {
		Scoping injection = scopingOf(Scoped.INJECTION);
		assertFalse(injection.isStableIn(Scoped.APPLICATION));
		assertFalse(injection.isStableIn(Scoped.DEPENDENCY));
		assertFalse(injection.isStableIn(Scoped.DEPENDENCY_INSTANCE));
		assertFalse(injection.isStableIn(Scoped.DEPENDENCY_TYPE));
		assertFalse(injection.isStableIn(Scoped.TARGET_INSTANCE));
		assertTrue(injection.isStableIn(injection));
	}

	@Test
	public void threadScopeIsNotStableInAnyOtherScopeExceptInjection() {
		Scoping thread = scopingOf(Scoped.THREAD);
		assertFalse(thread.isStableIn(Scoped.APPLICATION));
		assertFalse(thread.isStableIn(Scoped.DEPENDENCY));
		assertFalse(thread.isStableIn(Scoped.DEPENDENCY_INSTANCE));
		assertFalse(thread.isStableIn(Scoped.DEPENDENCY_TYPE));
		assertFalse(thread.isStableIn(Scoped.TARGET_INSTANCE));
		assertTrue(thread.isStableIn(thread));
		assertTrue(thread.isStableIn(Scoped.INJECTION));
	}

	@Test
	public void applicationIsStableInAnyOtherScope() {
		assertStableScope(Scoped.APPLICATION);
	}

	@Test
	public void dependencyBasedIsStableInAnyOtherScope() {
		assertStableScope(Scoped.DEPENDENCY);
		assertStableScope(Scoped.DEPENDENCY_INSTANCE);
		assertStableScope(Scoped.DEPENDENCY_TYPE);
		assertStableScope(Scoped.TARGET_INSTANCE);
	}

	private static void assertStableScope(Scope scope) {
		Scoping s = scopingOf(scope);
		assertTrue(s.isStableIn(scope));
		assertTrue(s.isStableIn(s));
		assertTrue(s.isStableIn(Scoped.DEPENDENCY));
		assertTrue(s.isStableIn(Scoped.DEPENDENCY_INSTANCE));
		assertTrue(s.isStableIn(Scoped.DEPENDENCY_TYPE));
		assertTrue(s.isStableIn(Scoped.TARGET_INSTANCE));
		assertTrue(s.isStableIn(Scoped.THREAD));
		assertTrue(s.isStableIn(Scoped.INJECTION));
	}

}
