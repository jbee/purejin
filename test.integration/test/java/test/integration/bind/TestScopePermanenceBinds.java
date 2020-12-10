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
import static se.jbee.inject.Cast.resourcesTypeFor;
import static se.jbee.inject.ScopePermanence.ignore;

/**
 * Verifies the {@link ScopePermanence} of the {@link DefaultScopes}.
 */
class TestScopePermanenceBinds {

	static final Name requestScope = Name.named("request");

	static final class TestScopePermanenceBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bindScopePermanence(requestScope).toFactory(
					context -> context.resolve(Scope.worker,
							ScopePermanence.class).derive(requestScope));
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestScopePermanenceBindsModule.class);

	private final Map<Name, ScopePermanence> permanenceByScope = new HashMap<>();

	@BeforeEach
	void setup() {
		for (ScopePermanence s : injector.resolve(ScopePermanence[].class))
			permanenceByScope.put(s.scope, s);
		permanenceByScope.put(ignore.scope, ignore);
	}

	private ScopePermanence getPermanence(Name scope) {
		return permanenceByScope.get(scope);
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
		assertWorkerScopePermanence(Scope.worker);
	}

	@Test
	void customWorkerScopeIsAsConsistentAsWorkerGroupScope() {
		assertWorkerScopePermanence(requestScope);
	}

	private void assertWorkerScopePermanence(Name worker) {
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
		assertPermanentScope(ScopePermanence.ignore);
	}

	@Test
	void defaultScopePermanencesAreBoundAsDefaults() {
		for (Resource<ScopePermanence> r : injector.resolve(
				resourcesTypeFor(ScopePermanence.class))) {
			if (!r.signature.instance.name.equalTo(requestScope))
				assertEquals(DeclarationType.DEFAULT, r.source.declarationType);
		}
	}

	@Test
	void defaultScopesAreBoundAsDefaults() {
		for (Resource<Scope> r : injector.resolve(
				resourcesTypeFor(Scope.class))) {
			if (!r.signature.instance.name.equalTo(requestScope))
				assertEquals(DeclarationType.DEFAULT, r.source.declarationType);
		}
	}

	private void assertIsConsistentIn(Name leftScope, Name rightScope) {
		assertTrue(getPermanence(leftScope).isConsistentIn(
				getPermanence(rightScope)));
	}

	private void assertIsNotConsistentIn(Name leftScope, Name rightScope) {
		assertFalse(getPermanence(leftScope).isConsistentIn(
				getPermanence(rightScope)));
	}

	private void assertPermanentScope(Name scope) {
		assertPermanentScope(getPermanence(scope));
	}

	private void assertPermanentScope(ScopePermanence tested) {
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
