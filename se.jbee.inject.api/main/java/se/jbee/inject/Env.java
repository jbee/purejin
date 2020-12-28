package se.jbee.inject;

import se.jbee.inject.lang.Cast;
import se.jbee.inject.lang.Reflect;
import se.jbee.inject.lang.Type;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.util.function.Function;

import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.raw;

/**
 * An {@link Env} is a like a key-value property map where the keys are {@link
 * Type}s and the values instances of the key type.
 * <p>
 * To distinguish multiple values of the same {@link Type} a {@link Name}
 * qualifier is added.
 * <p>
 * To avoid collisions between qualified properties as used within different
 * software modules that do not know about each other {@link Packages} are used
 * as namespaces.
 * <p>
 * Properties that are considered namespace specific are resolved providing that
 * {@link Package} namespace based on the {@link Class} that uses the property.
 * <p>
 * Properties that are considered "globally" used do not use a particular {@link
 * Package} as namespace which makes them "global".
 */
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

	String GP_BIND_BINDINGS = "self-bind";

	<T> T property(Name qualifier, Type<T> property, Class<?> ns)
			throws InconsistentDeclaration;

	default <T> T property(Class<T> property, Class<?> ns)
			throws InconsistentDeclaration {
		return property(Name.DEFAULT, raw(property), ns);
	}

	default <T> T property(Type<T> property, Class<?> ns)
			throws InconsistentDeclaration {
		return property(Name.DEFAULT, property, ns);
	}

	default <T> T property(String qualifier, Class<T> property, Class<?> ns) {
		return property(named(qualifier), raw(property), ns);
	}

	default <T> T property(String qualifier, Class<T> property, Class<?> ns, T defaultValue) {
		return property(named(qualifier), raw(property), ns, defaultValue);
	}

	default <T> T property(Name qualifier, Type<T> property, Class<?> ns, T defaultValue) {
		try {
			return property(qualifier, property, ns);
		} catch (InconsistentDeclaration e) {
			return defaultValue;
		}
	}

	/*
	 * Global Properties (properties in the "global" space)
	 *
	 * This simply means if they are defined they will have the same value
	 * independent of where they are resolved.
	 */

	default <T> T globalProperty(Type<T> property, T defaultValue) {
		return globalProperty(Name.DEFAULT.value, property, defaultValue);
	}

	default boolean globalProperty(String qualifier, boolean defaultValue) {
		return globalProperty(qualifier, raw(boolean.class), defaultValue);
	}

	default <T> T globalProperty(String qualifier, Type<T> property, T defaultValue) {
		try {
			return globalProperty(qualifier, property);
		} catch (InconsistentDeclaration e) {
			return defaultValue;
		}
	}

	default <T> T globalProperty(String qualifier, Type<T> property) {
		return property(named(qualifier), property, null);
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
				Reflect.accessible(target);
		}
	}

	default <T> Verifier verifierFor(T target) {
		if (!globalProperty(Env.GP_USE_VERIFICATION, false))
			return Verifier.AOK;
		@SuppressWarnings("unchecked")
		Class<T> targetClass = (Class<T>) target.getClass();
		Function<T, Verifier> factory = globalProperty(Cast.functionTypeOf(
				targetClass, Verifier.class), null);
		return factory == null ? Verifier.AOK : factory.apply(target);
	}
}
