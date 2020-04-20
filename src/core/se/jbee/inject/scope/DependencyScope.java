package se.jbee.inject.scope;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
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
			DependencyScope::instanceName);

	public static String typeName(Dependency<?> dep) {
		return dep.type().toString();
	}

	public static String instanceName(Dependency<?> dep) {
		return dep.instance.name.toString() + "@" + dep.type().toString();
	}

	public static String hierarchicalInstanceName(Dependency<?> dep) {
		return instanceName(dep) + targetInstanceName(dep);
	}

	public static String targetInstanceName(Dependency<?> dep) {
		StringBuilder b = new StringBuilder();
		for (int i = dep.injectionDepth() - 1; i >= 0; i--)
			b.append(dep.target(i));
		return b.toString();
	}

	/**
	 * Cannot use a {@link ConcurrentHashMap} for this since it has the property
	 * of not allowing updates to any other key while an entry being updated
	 * atomically but resolving dependencies during the update (in the update
	 * function) could very well lead to initialising other entries.
	 */
	private final ConcurrentMap<String, Object> instances = new ConcurrentSkipListMap<>();
	private final Function<Dependency<?>, String> injectionKey;

	public DependencyScope(Function<Dependency<?>, String> injectionKey) {
		this.injectionKey = injectionKey;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T provide(int serialID, Dependency<? super T> dep,
			Provider<T> provider, int generators)
			throws UnresolvableDependency {
		return (T) instances.computeIfAbsent(injectionKey.apply(dep),
				k -> provider.provide());
	}

}