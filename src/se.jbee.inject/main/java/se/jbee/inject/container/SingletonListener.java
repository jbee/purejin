package se.jbee.inject.container;

import se.jbee.inject.Injector;
import se.jbee.inject.Name;
import se.jbee.inject.Resource;
import se.jbee.inject.ScopePermanence;

/**
 * Listener interface invoked by the {@link Injector}. Implementations are bound
 * as part of the {@link Injector} context.
 * 
 * Keep in mind that any instance implementing {@link SingletonListener} is
 * created ahead of the tracking so these cannot be tracked even if they qualify
 * as stable instances.
 * 
 * @since 19.1
 */
@FunctionalInterface
public interface SingletonListener {

	/**
	 * Called by the {@link Injector} when an instance which is
	 * {@link ScopePermanence#isPermanent()} is created. These are typical
	 * "singleton" instances of an application.
	 * 
	 * @param <T> Type of the created instance
	 * @param resource the {@link Resource} descriptor that is the source of the
	 *            provided instance
	 * @param instance the created instance
	 */
	<T> void onSingletonCreated(Resource<T> resource, T instance);

	default SingletonListener inScope(Name scope) {
		SingletonListener self = this;
		return new SingletonListener() {

			@Override
			public <T> void onSingletonCreated(Resource<T> resource,
					T instance) {
				if (resource.permanence.scope.equalTo(scope)) {
					self.onSingletonCreated(resource, instance);
				}
			}
		};
	}
}
