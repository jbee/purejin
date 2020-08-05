/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import static se.jbee.inject.InconsistentDeclaration.annotationLacksProperty;
import static se.jbee.inject.lang.Utils.annotatedName;
import static se.jbee.inject.lang.Utils.annotation;
import static se.jbee.inject.lang.Utils.annotationPropertyByType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import se.jbee.inject.Name;

/**
 * Extracts the {@link Name} used for instance being bound.
 *
 * @since 19.1
 */
@FunctionalInterface
public interface NamesBy {

	/**
	 * @return The {@link Name} of the instance provided by the given object.
	 *         Use {@link Name#DEFAULT} for no specific name.
	 */
	Name reflect(AccessibleObject obj);

	NamesBy defaultName = obj -> Name.DEFAULT;

	static NamesBy memberNameOr(NamesBy fallback) {
		return obj -> {
			if (obj instanceof Member) {
				return Name.named(((Member) obj).getName());
			}
			return fallback.reflect(obj);
		};
	}

	default NamesBy unlessAnnotatedWith(Class<? extends Annotation> naming) {
		if (naming == null)
			return this;
		Method nameProperty = annotationPropertyByType(String.class, naming);
		if (nameProperty == null)
			throw annotationLacksProperty(String.class, naming);
		return obj -> {
			String name = annotatedName(nameProperty, annotation(naming, obj));
			return name == null ? this.reflect(obj) : Name.named(name);
		};
	}

}
