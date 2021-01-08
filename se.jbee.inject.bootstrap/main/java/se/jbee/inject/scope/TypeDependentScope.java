package se.jbee.inject.scope;

import se.jbee.inject.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;

/**
 * A {@link Scope} that maintains a map of instances where the key is derived
 * from the {@link Dependency} itself. In the simplest case this is the
 * {@link #typeSignature(Dependency)} or the
 * {@link #instanceSignature(Dependency)} but it can also include the injection
 * hierarchy as used by {@link #hierarchicalInstanceSignature(Dependency)}.
 *
 * One use case are {@link #JVM} singletons. As the scope is kept in a normal
 * constant that is shared within the JVM this effectively shares the map's
 * instances within the same JVM.
 *
 * @since 8.1
 */
public final class TypeDependentScope implements Scope {

	/**
	 * Effectively gives a JVM singleton per {@link Instance}.
	 *
	 * @since 8.1
	 */
	public static final Scope JVM = scope(TypeDependentScope::instanceSignature);

	public static Scope scope(Function<Dependency<?>, String> injectionKey) {
		return new TypeDependentScope(injectionKey);
	}

	public static Scope perTypeSignature() {
		return scope(TypeDependentScope::typeSignature);
	}

	public static Scope perInstanceSignature() {
		return scope(TypeDependentScope::instanceSignature);
	}

	public static Scope perHierarchicalInstanceSignature() {
		return scope(TypeDependentScope::hierarchicalInstanceSignature);
	}

	public static Scope perTargetInstanceSignature() {
		return scope(TypeDependentScope::targetInstanceSignature);
	}
	public static String typeSignature(Dependency<?> dep) {
		return dep.type().toString();
	}

	public static String instanceSignature(Dependency<?> dep) {
		return dep.instance.name.toString() + "@" + dep.type().toString();
	}

	public static String hierarchicalInstanceSignature(Dependency<?> dep) {
		return instanceSignature(dep) + targetInstanceSignature(dep);
	}

	public static String targetInstanceSignature(Dependency<?> dep) {
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

	public TypeDependentScope(Function<Dependency<?>, String> injectionKey) {
		this.injectionKey = injectionKey;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T provide(int serialID, int resources, Dependency<? super T> dep,
			Provider<T> provider) throws UnresolvableDependency {
		return (T) instances.computeIfAbsent(injectionKey.apply(dep),
				k -> provider.provide());
	}

}
