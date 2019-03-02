package se.jbee.inject.config;

import static se.jbee.inject.InconsistentBinding.noSuchAnnotationProperty;
import static se.jbee.inject.Utils.annotationPropertyByType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import se.jbee.inject.Name;

@FunctionalInterface
public interface NamingMirror {

	/**
	 * @return The {@link Name} of the instance provided by the given object.
	 *         Use {@link Name#DEFAULT} for no specific name.
	 */
	Name reflect(AccessibleObject obj);

	NamingMirror defaultName = obj -> Name.DEFAULT;

	default NamingMirror orAnnotatedBy(Class<? extends Annotation> naming) {
		if (naming == null)
			return this;
		Method nameProperty = annotationPropertyByType(String.class, naming);
		if (nameProperty == null)
			throw noSuchAnnotationProperty(String.class, naming);
		return obj -> {
			try {
				if (!obj.isAnnotationPresent(naming))
					return this.reflect(obj);
				String name = (String) nameProperty.invoke(
						obj.getAnnotation(naming));
				if (!name.isEmpty()
					&& !name.equals(nameProperty.getDefaultValue()))
					return Name.named(name);
				return this.reflect(obj);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				return this.reflect(obj);
			}
		};
	}

}