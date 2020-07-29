package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.container.Cast.resourcesTypeFor;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import se.jbee.inject.DeclarationType;
import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.ScopePermanence;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * Verifies the {@link ScopePermanence} of the {@link DefaultScopes}.
 */
public class TestScopePermanenceBinds {

	static final Name requestScope = Name.named("request");

	static final class TestScopingBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bindScopePermanence(
					DefaultScopes.WORKER_SCOPE_PERMANENCE.derive(requestScope));
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestScopingBindsModule.class);

	private final Map<Name, ScopePermanence> scopingsByName = new HashMap<>();

	@Before
	public void setup() {
		for (ScopePermanence s : injector.resolve(ScopePermanence[].class))
			scopingsByName.put(s.scope, s);
		scopingsByName.put(ScopePermanence.ignore.scope,
				ScopePermanence.ignore);
	}

	private ScopePermanence scopingOf(Name scope) {
		return scopingsByName.get(scope);
	}

	@Test
	public void injectionScopeIsNotStableInAnyOtherScope() {
		assertIsNotStableIn(Scope.injection, Scope.application);
		assertIsNotStableIn(Scope.injection, Scope.dependency);
		assertIsNotStableIn(Scope.injection, Scope.dependencyInstance);
		assertIsNotStableIn(Scope.injection, Scope.dependencyType);
		assertIsNotStableIn(Scope.injection, Scope.targetInstance);
		assertIsNotStableIn(Scope.injection, Scope.worker);
		assertIsNotStableIn(Scope.injection, Scope.thread);
		assertIsStableIn(Scope.injection, Scope.injection);
	}

	@Test
	public void threadScopeIsNotStableInAnyOtherScopeExceptInjectionAndWorker() {
		assertIsNotStableIn(Scope.thread, Scope.application);
		assertIsNotStableIn(Scope.thread, Scope.dependency);
		assertIsNotStableIn(Scope.thread, Scope.dependencyInstance);
		assertIsNotStableIn(Scope.thread, Scope.dependencyType);
		assertIsNotStableIn(Scope.thread, Scope.targetInstance);
		assertIsStableIn(Scope.thread, Scope.thread);
		assertIsStableIn(Scope.thread, Scope.injection);
		assertIsStableIn(Scope.thread, Scope.worker);
	}

	@Test
	public void workerScopeIsNotStableInAnyOtherScopeExceptInjection() {
		assertWorkerScopeStability(Scope.worker);
	}

	@Test
	public void customWorkerScopeIsAsStableAsWorkerGroupScope() {
		assertWorkerScopeStability(requestScope);
	}

	private void assertWorkerScopeStability(Name worker) {
		assertIsNotStableIn(worker, Scope.application);
		assertIsNotStableIn(worker, Scope.dependency);
		assertIsNotStableIn(worker, Scope.dependencyInstance);
		assertIsNotStableIn(worker, Scope.dependencyType);
		assertIsNotStableIn(worker, Scope.targetInstance);
		assertIsNotStableIn(worker, Scope.thread);
		assertIsStableIn(worker, worker);
		assertIsStableIn(worker, Scope.worker);
		assertIsStableIn(worker, Scope.injection);
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
		assertStableScope(ScopePermanence.ignore);
	}

	@Test
	public void defaultScopePermanencesAreBoundAsDefaults() {
		for (Resource<ScopePermanence> r : injector.resolve(
				resourcesTypeFor(ScopePermanence.class))) {
			if (!r.locator.instance.name.equalTo(requestScope))
				assertEquals(DeclarationType.DEFAULT, r.source.declarationType);
		}
	}

	@Test
	public void defaultScopesAreBoundAsDefaults() {
		for (Resource<Scope> r : injector.resolve(
				resourcesTypeFor(Scope.class))) {
			if (!r.locator.instance.name.equalTo(requestScope))
				assertEquals(DeclarationType.DEFAULT, r.source.declarationType);
		}
	}

	private void assertIsStableIn(Name leftScope, Name rightScope) {
		assertTrue(scopingOf(leftScope).isStableIn(scopingOf(rightScope)));
	}

	private void assertIsNotStableIn(Name leftScope, Name rightScope) {
		assertFalse(scopingOf(leftScope).isStableIn(scopingOf(rightScope)));
	}

	private void assertStableScope(Name scope) {
		assertStableScope(scopingOf(scope));
	}

	private void assertStableScope(ScopePermanence s) {
		assertTrue(s.isStableByNature());
		assertTrue(s.isStableIn(s));
		Name scope = s.scope;
		assertIsStableIn(scope, scope);
		assertIsStableIn(scope, s.scope);
		assertIsStableIn(scope, Scope.dependency);
		assertIsStableIn(scope, Scope.dependencyInstance);
		assertIsStableIn(scope, Scope.dependencyType);
		assertIsStableIn(scope, Scope.targetInstance);
		assertIsStableIn(scope, Scope.thread);
		assertIsStableIn(scope, Scope.injection);
	}

}
