package se.jbee.inject.container;

import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.ScopePermanence;

/**
 * Listener interface invoked by the {@link Injector}. Implementations are bound
 * as part of the {@link Injector} context.
 * 
 * Keep in mind that any instance implementing {@link PostConstructObserver} is
 * created ahead of the tracking so these cannot be tracked themselves even if
 * they qualify as instances in a permanent scope
 * ({@link ScopePermanence#isPermanent()}).
 * 
 * @since 19.1
 */
@FunctionalInterface
public interface PostConstructObserver {

	/**
	 * Called by an {@link Injector} context when an instance was created and
	 * after eventual post-construct initialisation have been applied.
	 * 
	 * Note that this method is only called for typical "singleton" instances of
	 * an application with a permanent scope
	 * ({@link ScopePermanence#isPermanent()}).
	 * 
	 * @param <T> Type of the created instance
	 * @param resource the {@link Resource} that is the source of the provided
	 *            instance
	 * @param instance the created instance to observe by this listener
	 */
	<T> void afterPostConstruct(Resource<T> resource, T instance);

	/**
	 * Observes only instances in the provided scope.
	 *
	 * @param scope The {@link Name} of the {@link Scope} to observe
	 * @return A new {@link PostConstructObserver} that is limited to observe
	 *         instances created in the provided scope
	 */
	default PostConstructObserver inScope(Name scope) {
		PostConstructObserver self = this;
		return new PostConstructObserver() {

			@Override
			public <T> void afterPostConstruct(Resource<T> resource,
					T instance) {
				if (resource.permanence.scope.equalTo(scope))
					self.afterPostConstruct(resource, instance);
			}
		};
	}

	/**
	 * Merges a list of {@link PostConstructObserver}s into a single
	 * {@link PostConstructObserver}
	 *
	 * @param observers a single, multiple or none
	 * @return a merged {@link PostConstructObserver}, {@code null} in case none
	 *         were provided
	 */
	static PostConstructObserver merge(PostConstructObserver... observers) {
		if (observers == null || observers.length == 0)
			return null;
		if (observers.length == 1)
			return observers[0];
		return new PostConstructObserver() {

			@Override
			public <T> void afterPostConstruct(Resource<T> resource,
					T instance) {
				for (PostConstructObserver observer : observers)
					observer.afterPostConstruct(resource, instance);
			}
		};
	}
}
