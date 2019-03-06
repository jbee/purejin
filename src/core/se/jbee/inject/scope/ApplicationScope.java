package se.jbee.inject.scope;

import java.util.concurrent.atomic.AtomicReference;

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

	private final AtomicReference<Object[]> instances = new AtomicReference<>();

	@SuppressWarnings("unchecked")
	@Override
	public <T> T yield(int serialID, Dependency<? super T> dep,
			Provider<T> provider, int generators)
			throws UnresolvableDependency {
		Object[] objects = instances.get();
		if (objects == null) {
			instances.compareAndSet(null, new Object[generators]);
			objects = instances.get();
		}
		Object res = objects[serialID];
		if (res != null)
			return (T) res;
		synchronized (objects) {
			res = objects[serialID];
			// we need to ask again since the instance could have been initialised before we got entrance to the sync block
			if (res == null) {
				res = provider.provide();
				objects[serialID] = res;
			}
		}
		return (T) res;
	}
}