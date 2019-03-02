package se.jbee.inject.config;

import static se.jbee.inject.InconsistentBinding.noSuchAnnotationProperty;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.parameterTypes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import se.jbee.inject.Array;
import se.jbee.inject.Name;
import se.jbee.inject.Parameter;
import se.jbee.inject.Type;

@FunctionalInterface
public interface ParameterisationMirror {

	/**
	 * @return The {@link Parameter} hints for the construction/invocation of
	 *         the given object. This is either a
	 *         {@link java.lang.reflect.Constructor} or a
	 *         {@link java.lang.reflect.Method} Use a zero length array if there
	 *         are no hits.
	 */
	Parameter<?>[] reflect(Executable obj);

	ParameterisationMirror noParameters = obj -> Parameter.noParameters;

	/**
	 * A {@link ParameterisationMirror} that allows to specify the
	 * {@link Annotation} which is used to indicate the instance {@link Name} of
	 * a method parameter.
	 */
	default ParameterisationMirror orNamesAnnotatedBy(
			Class<? extends Annotation> naming) {
		if (naming == null)
			return this;
		Method nameProperty = Annotations.methodReturning(String.class, naming);
		if (nameProperty == null)
			throw noSuchAnnotationProperty(String.class, naming);
		return obj -> {
			Annotation[][] ais = obj.getParameterAnnotations();
			Type<?>[] tis = parameterTypes(obj);
			Parameter<?>[] res = new Parameter[tis.length];
			int named = 0;
			for (int i = 0; i < res.length; i++) {
				res[i] = tis[i]; // default
				Annotation instance = Array.first(ais[i],
						a -> naming == a.annotationType());
				if (instance != null) {
					try {
						String name = (String) nameProperty.invoke(instance);
						if (!name.isEmpty()
							&& !name.equals(nameProperty.getDefaultValue())) {
							res[i] = instance(named(name), tis[i]);
							named++;
						}
					} catch (Exception e) {
						// gobble
					}
				}
			}
			return named == 0 ? this.reflect(obj) : res;

		};
	}
}
