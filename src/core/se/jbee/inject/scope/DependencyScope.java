package se.jbee.inject.scope;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import se.jbee.inject.Dependency;
import se.jbee.inject.Instance;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;

public final class DependencyScope implements Scope {

	/**
	 * Effectively gives a JVM singleton per {@link Instance}.
	 * 
	 * @since 19.1
	 */
	public static final Scope JVM = new DependencyScope(
			DependencyScope::dependencyInstanceOf);

	public static String dependencyTypeOf(Dependency<?> dep) {
		return dep.type().toString();
	}

	public static String dependencyInstanceOf(Dependency<?> dep) {
		return dep.instance.name.toString() + "@" + dep.type().toString();
	}

	public static String targetedDependencyTypeOf(Dependency<?> dep) {
		return dependencyInstanceOf(dep) + targetInstanceOf(dep);
	}

	public static String targetInstanceOf(Dependency<?> dep) {
		StringBuilder b = new StringBuilder();
		for (int i = dep.injectionDepth() - 1; i >= 0; i--)
			b.append(dep.target(i));
		return b.toString();
	}

	private final ConcurrentMap<String, Object> instances = new ConcurrentHashMap<>();
	private final Function<Dependency<?>, String> identity;

	public DependencyScope(Function<Dependency<?>, String> identity) {
		this.identity = identity;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T yield(int serialID, Dependency<? super T> dep,
			Provider<T> provider, int generators)
			throws UnresolvableDependency {
		final String key = identity.apply(dep);
		T instance = (T) instances.get(key);
		if (instance != null)
			return instance;
		synchronized (instances) {
			instance = (T) instances.get(key);
			if (instance == null) {
				instance = provider.provide();
				instances.put(key, instance);
			}
		}
		return instance;
	}

}