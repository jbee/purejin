package se.jbee.inject.container;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import se.jbee.inject.Provider;

/**
 * The {@link Lazy} utility class is used for fields that are initialised by a
 * {@link Provider} function that should only run once in case it will be
 * initialising the value.
 * 
 * It should not be run at all if the value is unidealised or another thread
 * will succeed in initialising it.
 * 
 * It should also not run multiple times to successful initialise the field.
 * 
 * @author Jan Bernitt
 *
 * @param <V> Type of the value
 */
public final class Lazy<V> extends AtomicReference<V> {

	private final AtomicBoolean uninitialised = new AtomicBoolean();

	public V get(Provider<V> initialValue) {
		V value;
		if (uninitialised.compareAndSet(false, true)) {
			value = initialValue.provide();
			set(value);
		} else {
			// another thread is about to initialise it
			do {
				value = get();
			} while (value == null);
		}
		return value;
	}
}
