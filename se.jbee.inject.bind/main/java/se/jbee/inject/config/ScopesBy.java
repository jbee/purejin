package se.jbee.inject.config;

import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.lang.TypeVariable;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.util.function.Function;

import static se.jbee.inject.Name.named;

/**
 * Extracts the {@link Name} of the {@link Scope} to use for instances of a
 * given type. This can be used to implement scope annotations.
 *
 * @since 8.1
 */
@FunctionalInterface
public interface ScopesBy {

	ScopesBy AUTO = target -> Scope.auto;

	ScopesBy RETURN_TYPE = target -> {
		if (target instanceof Method) {
			Method m = (Method) target;
			return TypeVariable.typeVariables(
					m.getGenericReturnType()).isEmpty()
					? Scope.application
					: Scope.dependencyType;
		}
		return Scope.application;
	};

	Name reflect(GenericDeclaration type);

	default ScopesBy orElse(Name name) {
		return obj -> {
			Name n = reflect(obj);
			return n != null ? n : name;
		};
	}

	default ScopesBy orElse(ScopesBy whenNull) {
		return obj -> {
			Name n = reflect(obj);
			return n != null ? n : whenNull.reflect(obj);
		};
	}

	static <T extends Annotation> ScopesBy annotatedWith(Class<T> annotation, Function<T, String> property) {
		return obj -> obj.isAnnotationPresent(annotation) //
				? named(property.apply(obj.getAnnotation(annotation)))
				: null;
	}
}
