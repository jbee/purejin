package test.integration.container;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.NoMethodForDependency;

import java.lang.reflect.Array;

public class Decorate {

	private Decorate() {
		throw new UnsupportedOperationException("util");
	}

	/**
	 * Chains {@link Injector}s similar to a {@link ClassLoader} hierarchy.
	 *
	 * @since 8.1
	 * @param root context acting as fallback
	 * @param branch context that is tried first when resolving dependencies
	 * @return An {@link Injector} with both contexts where root acts as
	 *         fallback
	 */
	public static Injector hierarchy(Injector root, Injector branch) {
		return new Injector() {

			@Override
			public <T> T resolve(Dependency<T> dep)
					throws UnresolvableDependency {
				if (dep.type().arrayDimensions() == 1)
					return Decorate.resolveArray(dep, root, branch);
				try {
					return branch.resolve(dep);
				} catch (UnresolvableDependency.ResourceResolutionFailed | NoMethodForDependency e) {
					return root.resolve(dep);
				}
			}

		};
	}

	@SuppressWarnings("unchecked")
	static <T> T resolveArray(Dependency<T> dep, Injector root,
			Injector branch) {
		T branchInstance = null;
		try {
			branchInstance = branch.resolve(dep);
		} catch (UnresolvableDependency e) {
			return root.resolve(dep);
		}
		T rootInstance = null;
		try {
			rootInstance = root.resolve(dep);
		} catch (UnresolvableDependency e) {
			return branchInstance;
		}
		int rootLength = Array.getLength(rootInstance);
		int branchLength = Array.getLength(branchInstance);
		Object arr = Array.newInstance(dep.type().baseType().rawType,
				rootLength + branchLength);
		System.arraycopy(rootInstance, 0, arr, 0, rootLength);
		System.arraycopy(branchInstance, 0, arr, rootLength, branchLength);
		return (T) arr;
	}
}
