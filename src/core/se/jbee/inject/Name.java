/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * A {@link Name} is used as discriminator in cases where multiple
 * {@link Instance}s are bound for the same {@link Type}.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Name implements MoreApplicableThan<Name>, Serializable {

	/**
	 * Character used as wildcard when matching names.
	 */
	public static final String WILDCARD = "*";

	/**
	 * Used when no name is specified. It is the most applicable name of all.
	 */
	public static final Name DEFAULT = new Name("");
	/**
	 * It is the least applicable name of all.
	 */
	public static final Name ANY = new Name(WILDCARD);

	private final String value;

	public static Name pluginFor(Class<?> pluginPoint, String name) {
		return named(pluginPoint.getCanonicalName() + ":" + name);
	}

	public static Name named(Object name) {
		return named(String.valueOf(name));
	}

	public static Name named(String name) {
		if (name == null || name.trim().isEmpty()) {
			return DEFAULT;
		}
		return new Name(name.toLowerCase());
	}

	private Name(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	public boolean equalTo(Name other) {
		return value.equals(other.value);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Name && equalTo((Name) obj);
	}

	public boolean isAny() {
		return value.equals(ANY.value);
	}

	public boolean isDefault() {
		return value.isEmpty();
	}

	@Override
	public boolean moreApplicableThan(Name other) {
		final boolean thisIsDefault = isDefault();
		final boolean otherIsDefault = other.isDefault();
		if (thisIsDefault || otherIsDefault) {
			return !otherIsDefault;
		}
		final boolean thisIsAny = isAny();
		final boolean otherIsAny = other.isAny();
		if (thisIsAny || otherIsAny) {
			return !thisIsAny;
		}
		return value.length() > other.value.length()
			&& value.startsWith(other.value);
	}

	public boolean isCompatibleWith(Name other) {
		return isAny() || other.isAny() || other.value.equals(value)
			|| (value.matches(other.value.replace(WILDCARD, ".*")));
	}

	public static Name namedBy(Class<? extends Annotation> annotation,
			AnnotatedElement obj) {
		return annotation == null || !obj.isAnnotationPresent(annotation)
			? Name.DEFAULT
			: namedBy(annotation, obj.getAnnotation(annotation));
	}

	public static Name namedBy(Class<? extends Annotation> annotation,
			Annotation... instances) {
		for (Annotation i : instances) {
			if (i.annotationType() == annotation) {
				return namedBy(annotation, i);
			}
		}
		return Name.DEFAULT;
	}

	private static Name namedBy(Class<? extends Annotation> annotation,
			Annotation instance) {
		for (Method m : annotation.getDeclaredMethods()) {
			if (String.class == m.getReturnType()) {
				String name = null;
				try {
					name = (String) m.invoke(instance);
				} catch (Exception e) {
					// try next...
				}
				if (name != null && !name.isEmpty()
					&& !name.equals(m.getDefaultValue())) {
					return Name.named(name);
				}
			}
		}
		return Name.DEFAULT;
	}
}
