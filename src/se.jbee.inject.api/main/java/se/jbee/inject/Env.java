package se.jbee.inject;

import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;

import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.raw;

@FunctionalInterface
public interface Env {

	/**
	 * Property name used to configure a boolean if reflection is allowed to
	 * make private members accessible using
	 * {@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}
	 */
	String GP_USE_DEEP_REFLECTION = "deep-reflection";
	/**
	 * Property name used to configure the set of {@link Packages} where deep
	 * reflection is allowed given {@link #GP_USE_DEEP_REFLECTION} is true.
	 */
	String GP_DEEP_REFLECTION_PACKAGES = "deep-reflection-packages";

	<T> T property(Name name, Type<T> property, Package scope)
			throws InconsistentDeclaration;

	default <T> T property(Class<T> property, Package scope)
			throws InconsistentDeclaration {
		return property(Name.DEFAULT, raw(property), scope);
	}

	default <T> T property(Type<T> property, Package scope)
			throws InconsistentDeclaration {
		return property(Name.DEFAULT, property, scope);
	}

	default boolean globalProperty(String name, boolean defaultValue) {
		try {
			return globalProperty(name, raw(boolean.class));
		} catch (InconsistentDeclaration e) {
			return defaultValue;
		}
	}

	default <T> T globalProperty(String name, Type<T> property) {
		return property(named(name), property, null);
	}

	default <T> T globalProperty(Type<T> property) {
		return property(property, null);
	}

	default <T> T globalProperty(Class<T> property) {
		return property(property, null);
	}

	default <E extends Enum<E>> boolean toggled(Class<E> property, E feature,
			Package scope) {
		try {
			if (feature == null) {
				return property(named("null"), raw(property), scope) == null;
			}
			return property(named(feature.name()),
					raw(feature.getDeclaringClass()), scope) != null;
		} catch (InconsistentDeclaration e) {
			return false;
		}
	}

	default <T extends  AccessibleObject & Member> void accessible(T target) {
		if (globalProperty(Env.GP_USE_DEEP_REFLECTION, false)) {
			Packages where = globalProperty(Env.GP_DEEP_REFLECTION_PACKAGES,
					raw(Packages.class));
			if (where.contains(target.getDeclaringClass()))
				Utils.accessible(target);
		}
	}
}
