package se.jbee.inject.defaults;

import se.jbee.inject.*;
import se.jbee.inject.bind.Bind;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.scope.*;

import static se.jbee.inject.ScopeLifeCycle.singleton;
import static se.jbee.inject.ScopeLifeCycle.unstable;

/**
 * Binds implementations for the standard {@link Scope}s declared as
 * {@link Name} in the {@link Scope} class.
 */
public final class DefaultScopes extends BinderModule {

	@Override
	protected Bind init(Bind bind) {
		return bind.asDefault();
	}

	@Override
	protected void declare() {
		bindLifeCycle(ScopeLifeCycle.reference);
		bindLifeCycle(ScopeLifeCycle.container);
		bindLifeCycle(singleton.derive(Scope.jvm));
		bindLifeCycle(singleton.derive(Scope.application));
		bindLifeCycle(singleton.derive(Scope.dependency));
		bindLifeCycle(singleton.derive(Scope.dependencyType));
		bindLifeCycle(singleton.derive(Scope.dependencyInstance));
		bindLifeCycle(singleton.derive(Scope.targetInstance));
		bindLifeCycle(unstable.derive(Scope.thread) //
				.canBeInjectedInto(Scope.thread)//
				.canBeInjectedInto(Scope.worker) //
				.canBeInjectedInto(Scope.injection)); //
		bindLifeCycle(unstable.derive(Scope.injection) //
				.canBeInjectedInto(Scope.injection));
		bindLifeCycle(unstable.derive(Scope.worker) //
				.canBeInjectedInto(Scope.worker) //
				.canBeInjectedInto(Scope.injection)); //

		bindScope(Scope.injection).to(Scope.INJECTION);
		bindScope(Scope.application).to(ApplicationScope.class);
		bindScope(Scope.thread).to(ThreadScope.class);
		bindScope(Scope.jvm).to(TypeDependentScope.JVM);
		bindScope(Scope.worker).to(WorkerScope.class);
		per(Scope.worker).bind(
				Scope.Controller.forScope(Scope.worker)).toGenerator(
						gen -> null); // dummy generator as the scope will supply

		bindScope(Scope.dependency).toProvider(TypeDependentScope::perHierarchicalInstanceSignature);
		bindScope(Scope.dependencyInstance).toProvider(TypeDependentScope::perInstanceSignature);
		bindScope(Scope.dependencyType).toProvider(TypeDependentScope::perTypeSignature);
		bindScope(Scope.targetInstance).toProvider(TypeDependentScope::perTargetInstanceSignature);


	}

}
