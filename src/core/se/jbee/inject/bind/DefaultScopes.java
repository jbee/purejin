package se.jbee.inject.bind;

import static se.jbee.inject.Name.named;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.config.Config;
import se.jbee.inject.container.Supplier;
import se.jbee.inject.scope.ApplicationScope;
import se.jbee.inject.scope.DependencyScope;
import se.jbee.inject.scope.DiskScope;
import se.jbee.inject.scope.ThreadScope;

public final class DefaultScopes extends BinderModule
		implements Supplier<Scope> {

	@Override
	protected void declare() {
		ScopedBinder inScope = asDefault().per(Scope.container);
		inScope.bind(Scope.injection, Scope.class).to(Scope.INJECTION);
		inScope.bind(Scope.application, Scope.class).to(ApplicationScope.class);
		inScope.bind(Scope.thread, Scope.class).to(ThreadScope.class);
		inScope.bind(Scope.jvm, Scope.class).to(DependencyScope.JVM);
		inScope.bind(Scope.dependency, Scope.class).to(
				() -> new DependencyScope(
						DependencyScope::hierarchicalInstanceName));
		inScope.bind(Scope.dependencyInstance, Scope.class).to(
				() -> new DependencyScope(DependencyScope::instanceName));
		inScope.bind(Scope.dependencyType, Scope.class).to(
				() -> new DependencyScope(DependencyScope::typeName));
		inScope.bind(Scope.targetInstance, Scope.class).to(
				() -> new DependencyScope(DependencyScope::targetInstanceName));
		inScope.bind(named("disk:*"), Scope.class).toSupplier(this);
		inScope.bind(ScheduledExecutorService.class).to(
				Executors::newSingleThreadScheduledExecutor);
	}

	/**
	 * By convention {@link Scope#disk(File)} create names starting with
	 * {@code disk:} followed by the path to the folder of the scope. This can
	 * be used to extract the directory in this {@link Supplier} to bind the
	 * particular {@link DiskScope} for that directory.
	 */
	@Override
	public Scope supply(Dependency<? super Scope> dep, Injector context)
			throws UnresolvableDependency {
		String disk = dep.instance.name.toString();
		File dir = new File(disk.substring(5));
		return new DiskScope(context.resolve(Config.class),
				context.resolve(ScheduledExecutorService.class), dir,
				DependencyScope::instanceName);
	}

}