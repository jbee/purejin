package se.jbee.inject;

import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.util.function.Function;

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

	/**
	 * Property name used to configure a boolean if {@link Verifier}s are tried
	 * to be resolved during the binding process. If not, {@link Verifier#AOK}
	 * (no validation) is used (default).
	 */
	String GP_USE_VERIFICATION = "verify";

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

	/*
	 * Global Properties (properties in the "global" scope)
	 *
	 * This simply means if they are defined they will have the same value
	 * independent of where they are resolved.
	 */

	default <T> T globalProperty(Type<T> property, T defaultValue) {
		return globalProperty(Name.DEFAULT.value, property, defaultValue);
	}

	default boolean globalProperty(String name, boolean defaultValue) {
		return globalProperty(name, raw(boolean.class), defaultValue);
	}

	default <T> T globalProperty(String name, Type<T> property, T defaultValue) {
		try {
			return globalProperty(name, property);
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

	default <E extends Enum<E>> boolean toggled(Class<E> property, E feature) {
		try {
			if (feature == null) {
				return globalProperty("null", raw(property)) == null;
			}
			return globalProperty(feature.name(),
					raw(feature.getDeclaringClass())) != null;
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

	default <T> Verifier verifierFor(T target) {
		if (!globalProperty(Env.GP_USE_VERIFICATION, false))
			return Verifier.AOK;
		Class<T> targetClass = (Class<T>) target.getClass();
		Function<T, Verifier> factory = globalProperty(Cast.functionTypeOf(
				targetClass, Verifier.class), null);
		return factory == null ? Verifier.AOK : factory.apply(target);
	}
}
