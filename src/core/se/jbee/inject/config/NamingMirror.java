package se.jbee.inject.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;

import se.jbee.inject.Name;

@FunctionalInterface
public interface NamingMirror {

	/**
	 * @return The {@link Name} of the instance provided by the given object.
	 *         Use {@link Name#DEFAULT} for no specific name.
	 */
	Name reflect(AccessibleObject obj);

	NamingMirror defaultName = obj -> Name.DEFAULT;

	static NamingMirror annotatedAsValueOf(Class<? extends Annotation> annotation) {
		return obj -> Name.namedBy(annotation, obj);
	}

}