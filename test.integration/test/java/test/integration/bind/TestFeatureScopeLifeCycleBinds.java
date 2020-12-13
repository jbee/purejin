package test.integration.bind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.DefaultScopes;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.inject.ScopeLifeCycle.ignore;

/**
 * Verifies the {@link ScopeLifeCycle} of the {@link DefaultScopes}.
 *
 * BTW both {@link Scope}s and {@link ScopeLifeCycle}s are managed instances
 * that can be resolved in the {@link Injector} context.
 *
 * {@link Scope} implementation may also have dependencies.
 */
class TestFeatureScopeLifeCycleBinds {

	static final Name requestScope = Name.named("request");

	static final class TestFeatureScopeLifeCycleBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bindLifeCycle(requestScope).toFactory(
					context -> context.resolve(Scope.worker,
							ScopeLifeCycle.class).derive(requestScope));
		}

	}

	private final Injector context = Bootstrap.injector(
			TestFeatureScopeLifeCycleBindsModule.class);

	private final Map<Name, ScopeLifeCycle> lifeCycleByScope = new HashMap<>();

	@BeforeEach
	void setup() {
		for (ScopeLifeCycle s : context.resolve(ScopeLifeCycle[].class))
			lifeCycleByScope.put(s.scope, s);
		lifeCycleByScope.put(ignore.scope, ignore);
	}

	private ScopeLifeCycle getLifeCycle(Name scope) {
		return lifeCycleByScope.get(scope);
	}

	@Test
	void injectionScopeIsNotConsistentInAnyOtherScope() {
		assertIsNotConsistentIn(Scope.injection, Scope.application);
		assertIsNotConsistentIn(Scope.injection, Scope.dependency);
		assertIsNotConsistentIn(Scope.injection, Scope.dependencyInstance);
		assertIsNotConsistentIn(Scope.injection, Scope.dependencyType);
		assertIsNotConsistentIn(Scope.injection, Scope.targetInstance);
		assertIsNotConsistentIn(Scope.injection, Scope.worker);
		assertIsNotConsistentIn(Scope.injection, Scope.thread);
		assertIsConsistentIn(Scope.injection, Scope.injection);
	}

	@Test
	void threadScopeIsNotConsistentInAnyOtherScopeExceptInjectionAndWorker() {
		assertIsNotConsistentIn(Scope.thread, Scope.application);
		assertIsNotConsistentIn(Scope.thread, Scope.dependency);
		assertIsNotConsistentIn(Scope.thread, Scope.dependencyInstance);
		assertIsNotConsistentIn(Scope.thread, Scope.dependencyType);
		assertIsNotConsistentIn(Scope.thread, Scope.targetInstance);
		assertIsConsistentIn(Scope.thread, Scope.thread);
		assertIsConsistentIn(Scope.thread, Scope.injection);
		assertIsConsistentIn(Scope.thread, Scope.worker);
	}

	@Test
	void workerScopeIsNotConsistentInAnyOtherScopeExceptInjection() {
		assertWorkerScopeLifeCycle(Scope.worker);
	}

	@Test
	void customWorkerScopeIsAsConsistentAsWorkerGroupScope() {
		assertWorkerScopeLifeCycle(requestScope);
	}

	private void assertWorkerScopeLifeCycle(Name worker) {
		assertIsNotConsistentIn(worker, Scope.application);
		assertIsNotConsistentIn(worker, Scope.dependency);
		assertIsNotConsistentIn(worker, Scope.dependencyInstance);
		assertIsNotConsistentIn(worker, Scope.dependencyType);
		assertIsNotConsistentIn(worker, Scope.targetInstance);
		assertIsNotConsistentIn(worker, Scope.thread);
		assertIsConsistentIn(worker, worker);
		assertIsConsistentIn(worker, Scope.worker);
		assertIsConsistentIn(worker, Scope.injection);
	}

	@Test
	void applicationIsConsistentInAnyOtherScope() {
		assertPermanentScope(Scope.application);
	}

	@Test
	void dependencyBasedIsConsistentInAnyOtherScope() {
		assertPermanentScope(Scope.dependency);
		assertPermanentScope(Scope.dependencyInstance);
		assertPermanentScope(Scope.dependencyType);
		assertPermanentScope(Scope.targetInstance);
	}

	@Test
	void ignoreIsConsistentInAnyOtherScope() {
		assertPermanentScope(ScopeLifeCycle.ignore);
	}

	@Test
	void defaultScopeLifeCyclesAreBoundAsDefaults() {
		for (Resource<ScopeLifeCycle> r : context.resolve(
				Resource.resourcesTypeOf(ScopeLifeCycle.class))) {
			if (!r.signature.instance.name.equalTo(requestScope))
				assertEquals(DeclarationType.DEFAULT, r.source.declarationType);
		}
	}

	@Test
	void defaultScopesAreBoundAsDefaults() {
		for (Resource<Scope> r : context.resolve(
				Resource.resourcesTypeOf(Scope.class))) {
			if (!r.signature.instance.name.equalTo(requestScope))
				assertEquals(DeclarationType.DEFAULT, r.source.declarationType);
		}
	}

	private void assertIsConsistentIn(Name leftScope, Name rightScope) {
		assertTrue(getLifeCycle(leftScope) //
				.isConsistentIn(getLifeCycle(rightScope)));
	}

	private void assertIsNotConsistentIn(Name leftScope, Name rightScope) {
		assertFalse(getLifeCycle(leftScope) //
				.isConsistentIn(getLifeCycle(rightScope)));
	}

	private void assertPermanentScope(Name scope) {
		assertPermanentScope(getLifeCycle(scope));
	}

	private void assertPermanentScope(ScopeLifeCycle tested) {
		assertTrue(tested.isPermanent());
		assertTrue(tested.isConsistentIn(tested));
		Name scope = tested.scope;
		assertIsConsistentIn(scope, scope);
		assertIsConsistentIn(scope, tested.scope);
		assertIsConsistentIn(scope, Scope.dependency);
		assertIsConsistentIn(scope, Scope.dependencyInstance);
		assertIsConsistentIn(scope, Scope.dependencyType);
		assertIsConsistentIn(scope, Scope.targetInstance);
		assertIsConsistentIn(scope, Scope.thread);
		assertIsConsistentIn(scope, Scope.injection);
	}

}
