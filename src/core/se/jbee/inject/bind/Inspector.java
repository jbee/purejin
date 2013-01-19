package se.jbee.inject.bind;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;

import se.jbee.inject.Name;
import se.jbee.inject.Parameter;

/**
 * A strategy to extract missing information from types that is used within the {@link Binder} to
 * allow semi-automatic bindings.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public interface Inspector {

	/**
	 * @return The {@link Member}s that should be bound from the given implementor.
	 */
	<T> AccessibleObject[] inspect( Class<T> implementor );

	Name nameFor( AccessibleObject obj );

	Parameter<?>[] parametersFor( AccessibleObject obj );
}
