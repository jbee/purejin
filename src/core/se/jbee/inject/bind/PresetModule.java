package se.jbee.inject.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface PresetModule<T> {

	/**
	 * @param bindings
	 *            use to declare made bound within this {@link Module}.
	 * @param inspector
	 *            the chosen strategy to pick the {@link Constructor}s or {@link Method}s used to
	 *            create instances.
	 */
	void declare( Bindings bindings, Inspector inspector, T preset );
}
