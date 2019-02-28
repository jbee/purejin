package se.jbee.inject.config;

import static se.jbee.inject.Parameter.parametersFor;
import static se.jbee.inject.Type.parameterTypes;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import se.jbee.inject.Name;
import se.jbee.inject.Parameter;

@FunctionalInterface
public interface ParameterisationMirror {

	/**
	 * @return The {@link Parameter} hints for the construction/invocation of
	 *         the given object. This is either a
	 *         {@link java.lang.reflect.Constructor} or a
	 *         {@link java.lang.reflect.Method} Use a zero length array if there
	 *         are no hits.
	 */
	Parameter<?>[] reflect(AccessibleObject obj);

	ParameterisationMirror DEFAULT = obj -> Parameter.NO_PARAMETERS;

	/**
	 * A {@link ParameterisationMirror} that allows to specify the
	 * {@link Annotation} which is used to indicate the instance {@link Name} of
	 * a method parameter.
	 */
	static ParameterisationMirror namedBy(
			Class<? extends Annotation> annotation) {
		return obj -> {
			if (annotation == null)
				return Parameter.NO_PARAMETERS;
			if (obj instanceof Method) {
				Method method = (Method) obj;
				return parametersFor(parameterTypes(method),
						method.getParameterAnnotations(), annotation);
			}
			if (obj instanceof Constructor<?>) {
				Constructor<?> constructor = (Constructor<?>) obj;
				return parametersFor(parameterTypes(constructor),
						constructor.getParameterAnnotations(), annotation);
			}
			return Parameter.NO_PARAMETERS;

		};
	}

}
