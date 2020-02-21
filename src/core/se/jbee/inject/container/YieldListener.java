package se.jbee.inject.container;

import se.jbee.inject.Generator;
import se.jbee.inject.Injector;
import se.jbee.inject.Resource;
import se.jbee.inject.Scoping;

/**
 * Listener interface invoked by the {@link Injector}. Implementations are bound
 * as part of the {@link Injector} context.
 * 
 * Keep in mind that any instance implementing {@link YieldListener} is created
 * ahead of the tracking so these cannot be tracked even if they qualify as
 * stable instances.
 * 
 * @since 19.1
 */
public interface YieldListener {

	/**
	 * Called by the {@link Injector} when an instance which is
	 * {@link Scoping#isStableByDesign()} is created. These are typical
	 * "singleton" instances of an application.
	 * 
	 * @param <T> Type of the created instance
	 * @param serialID {@link Injector} internal ID for the {@link Generator}
	 *            that created the instance
	 * @param resource the {@link Resource} representing the created instance
	 * @param scoping the {@link Scoping} of the created instance
	 * @param instance the created instance
	 */
	<T> void onStableInstanceGeneration(int serialID, Resource<T> resource,
			Scoping scoping, T instance);
}
