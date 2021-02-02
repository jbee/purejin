package se.jbee.inject.disk;

import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Installs;
import se.jbee.inject.schedule.Scheduled;
import se.jbee.inject.schedule.SchedulerModule;
import se.jbee.inject.scope.TypeDependentScope;

import java.io.File;

import static se.jbee.inject.Name.named;
import static se.jbee.inject.config.ProducesBy.declaredMethods;

@Installs(bundles = SchedulerModule.class)
public final class DiskScopeModule extends BinderModule {

	@Override
	protected void declare() {
		bindLifeCycle(ScopeLifeCycle.disk);
		bindScope(Name.ANY.in("disk")) //
				.toSupplier(DiskScopeModule::createDiskScope);
		connect(declaredMethods(false).annotatedWith(Scheduled.class)) //
				.inAny(DiskScope.class) //
				.asScheduled(named(Scheduled.class));
	}

	/**
	 * By convention {@link Scope#disk(File)} create names starting with
	 * {@code disk:} followed by the path to the folder of the scope. This can
	 * be used to extract the directory in this {@link Supplier} to bind the
	 * particular {@link DiskScope} for that directory.
	 */
	private static Scope createDiskScope(Dependency<? super Scope> dep, Injector context) {
		return new DiskScope(new File(dep.instance.name.withoutNamespace()),
				TypeDependentScope::instanceSignature);
	}
}
