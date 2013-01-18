package se.jbee.inject.bind;

import java.lang.reflect.AccessibleObject;

import se.jbee.inject.Name;
import se.jbee.inject.Parameter;

/**
 * Utility class containing common {@link Inspector} implementations.
 * 
 * To 'inspect' means to use reflection or similar techniques to extract information from types.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 */
public class Inspected {

	public static final Inspector METHODS = new Inspector() {

		@Override
		public Parameter<?>[] parametersFor( AccessibleObject obj ) {
			return new Parameter<?>[0];
		}

		@Override
		public Name nameFor( AccessibleObject obj ) {
			return Name.DEFAULT;
		}

		@Override
		public AccessibleObject[] inspect( Class<?> implementor ) {
			return implementor.getDeclaredMethods();
		}
	};
}
