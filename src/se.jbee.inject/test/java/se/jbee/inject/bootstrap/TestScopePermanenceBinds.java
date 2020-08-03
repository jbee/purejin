package se.jbee.inject.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.jbee.inject.Cast.resourcesTypeFor;
import static se.jbee.inject.ScopePermanence.ignore;

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
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.DefaultScopes;

/**
 * Verifies the {@link ScopePermanence} of the {@link DefaultScopes}.
 */
public class TestScopePermanenceBinds {

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

	@Before
	public void setup() {
		for (ScopePermanence s : injector.resolve(ScopePermanence[].class))
			permanenceByScope.put(s.scope, s);
		permanenceByScope.put(ignore.scope, ignore);
	}

	private ScopePermanence getPermanence(Name scope) {
		return permanenceByScope.get(scope);
	}

	@Test
	public void injectionScopeIsNotConsistentInAnyOtherScope() {
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
	public void threadScopeIsNotConsistentInAnyOtherScopeExceptInjectionAndWorker() {
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
	public void workerScopeIsNotConsistentInAnyOtherScopeExceptInjection() {
		assertWorkerScopePermanence(Scope.worker);
	}

	@Test
	public void customWorkerScopeIsAsConsistentAsWorkerGroupScope() {
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
	public void applicationIsConsistentInAnyOtherScope() {
		assertPermanentScope(Scope.application);
	}

	@Test
	public void dependencyBasedIsConsistentInAnyOtherScope() {
		assertPermanentScope(Scope.dependency);
		assertPermanentScope(Scope.dependencyInstance);
		assertPermanentScope(Scope.dependencyType);
		assertPermanentScope(Scope.targetInstance);
	}

	@Test
	public void ignoreIsConsistentInAnyOtherScope() {
		assertPermanentScope(ScopePermanence.ignore);
	}

	@Test
	public void defaultScopePermanencesAreBoundAsDefaults() {
		for (Resource<ScopePermanence> r : injector.resolve(
				resourcesTypeFor(ScopePermanence.class))) {
			if (!r.signature.instance.name.equalTo(requestScope))
				assertEquals(DeclarationType.DEFAULT, r.source.declarationType);
		}
	}

	@Test
	public void defaultScopesAreBoundAsDefaults() {
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
