package se.jbee.inject.scope;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;

/**
 * Asks the {@link Provider} once per binding. Thereby instances become
 * singletons local to the application.
 * 
 * Will lead to instances that can be seen as application-wide-singletons.
 * 
 * Contains an instance per {@link Generator}. Instances are never updated.
 */
public final class ApplicationScope implements Scope {

	private final AtomicReference<AtomicReferenceArray<Object>> instances = new AtomicReference<>();

	@SuppressWarnings("unchecked")
	@Override
	public <T> T yield(int serialID, Dependency<? super T> dep,
			Provider<T> provider, int generators)
			throws UnresolvableDependency {
		return (T) instances.updateAndGet(objs -> objs != null
			? objs
			: new AtomicReferenceArray<>(generators)).updateAndGet(serialID,
					obj -> obj != null ? obj : provider.provide());
	}
}