package se.jbee.inject.bind;

import se.jbee.inject.Scope;
import se.jbee.inject.scope.ApplicationScope;
import se.jbee.inject.scope.DependencyScope;
import se.jbee.inject.scope.ThreadScope;

public final class DefaultScopes extends BinderModule {

	@Override
	protected void declare() {
		ScopedBinder inScope = asDefault().per(Scope.container);
		inScope.bind(Scope.injection, Scope.class).to(Scope.INJECTION);
		inScope.bind(Scope.application, Scope.class).to(ApplicationScope.class);
		inScope.bind(Scope.thread, Scope.class).to(ThreadScope.class);
		inScope.bind(Scope.jvm, Scope.class).to(DependencyScope.JVM);
		inScope.bind(Scope.dependency, Scope.class).to(
				() -> new DependencyScope(
						DependencyScope::targetedDependencyTypeOf));
		inScope.bind(Scope.dependencyInstance, Scope.class).to(
				() -> new DependencyScope(
						DependencyScope::dependencyInstanceOf));
		inScope.bind(Scope.dependencyType, Scope.class).to(
				() -> new DependencyScope(DependencyScope::dependencyTypeOf));
		inScope.bind(Scope.targetInstance, Scope.class).to(
				() -> new DependencyScope(DependencyScope::targetInstanceOf));
	}

}