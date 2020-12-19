package se.jbee.inject.scope;

import se.jbee.inject.*;

/**
 * Asks the {@link Provider} once per thread per {@link Resource} which is
 * understand commonly as a usual 'per-thread' singleton.
 */
public final class ThreadScope implements Scope {

	private final ThreadLocal<Object[]> instances = new ThreadLocal<>();

	@SuppressWarnings("unchecked")
	@Override
	public <T> T provide(int serialID, int resources, Dependency<? super T> dep,
			Provider<T> provider) throws UnresolvableDependency {
		Object[] objects = instances.get();
		if (objects == null) {
			objects = new Object[resources];
			instances.set(objects);
		}
		Object res = objects[serialID];
		if (res == null) {
			res = provider.provide();
			objects[serialID] = res;
		}
		return (T) res;
	}
}
