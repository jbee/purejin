package se.jbee.inject.config;

import static se.jbee.inject.InconsistentDeclaration.annotationLacksProperty;
import static se.jbee.inject.lang.Utils.annotatedName;
import static se.jbee.inject.lang.Utils.annotation;
import static se.jbee.inject.lang.Utils.annotationPropertyByType;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;

import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.lang.TypeVariable;

/**
 * Extracts the {@link Name} of the {@link Scope} to use for instances of a
 * given type. This can be used to implement scope annotations.
 *
 * @since 8.1
 */
@FunctionalInterface
public interface ScopesBy {

	Name reflect(GenericDeclaration type);

	ScopesBy alwaysDefault = target -> Scope.auto;

	ScopesBy type = target -> {
		if (target instanceof Method) {
			Method m = (Method) target;
			return TypeVariable.typeVariables(
					m.getGenericReturnType()).isEmpty()
						? Scope.application
						: Scope.dependencyType;
		}
		return Scope.application;
	};

	default ScopesBy unlessAnnotatedWith(Class<? extends Annotation> naming) {
		if (naming == null)
			return this;
		Method nameProperty = annotationPropertyByType(String.class, naming);
		if (nameProperty == null)
			throw annotationLacksProperty(String.class, naming);
		return type -> {
			String name = annotatedName(nameProperty, annotation(naming, type));
			return name == null ? this.reflect(type) : Name.named(name);
		};
	}

}
