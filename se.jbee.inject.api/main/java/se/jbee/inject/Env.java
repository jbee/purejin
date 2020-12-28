package se.jbee.inject;

import se.jbee.inject.lang.Cast;
import se.jbee.inject.lang.Reflect;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;

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

	default <T> T property(Class<T> property) {
		return property(raw(property));
	}

	default <T> T property(Type<T> property) {
		return property(Name.DEFAULT, property, null);
	}

	default <T> T property(Class<T> property, T defaultValue) {
		return property(raw(property), defaultValue);
	}

	default <T> T property(Type<T> property, T defaultValue) {
		return property(Name.DEFAULT.value, property, defaultValue);
	}

	default boolean property(String qualifier, boolean defaultValue) {
		return property(qualifier, raw(boolean.class), defaultValue);
	}

	default <T> T property(String qualifier, Class<T> property) {
		return property(named(qualifier), raw(property), null);
	}

	default <T> T property(String qualifier, Class<T> property, T defaultValue) {
		return property(qualifier, raw(property), defaultValue);
	}

	default <T> T property(String qualifier, Type<T> property, T defaultValue) {
		return Utils.orElse(defaultValue,
				() ->  property(named(qualifier), property, null));
	}

	default Env in(Class<?> ns) {
		final class EnvInNs implements Env {

			final Env env;
			final Class<?> ns;

			EnvInNs(Env env, Class<?> ns) {
				this.env = env;
				this.ns = ns;
			}

			@Override
			public Env in(Class<?> ns) {
				return ns == null ? env : ns == this.ns ? this : new EnvInNs(env, ns);
			}

			@Override
			public <T> T property(Name qualifier, Type<T> property, Class<?> ns) {
				return env.property(qualifier, property, this.ns);
			}

			@Override
			public String toString() {
				return "Env[" + ns.getName() + "]";
			}
		}
		return new EnvInNs(this, ns);
	}

	default <E extends Enum<E>> boolean toggled(Class<E> property, E feature) {
		return feature == null
				? property("null", property, property.getEnumConstants()[0]) == null
				: property(feature.name(), property,null) != null;
	}

	default <T extends  AccessibleObject & Member> void accessible(T target) {
		if (property(Env.GP_USE_DEEP_REFLECTION, false)) {
			Packages where = property(Env.GP_DEEP_REFLECTION_PACKAGES,
					Packages.class);
			if (where.contains(target.getDeclaringClass()))
				Reflect.accessible(target);
		}
	}

	default <T> Verifier verifierFor(T target) {
		if (!property(Env.GP_USE_VERIFICATION, false))
			return Verifier.AOK;
		@SuppressWarnings("unchecked")
		Class<T> targetClass = (Class<T>) target.getClass();
		Function<T, Verifier> factory = property(Cast.functionTypeOf(
				targetClass, Verifier.class), null);
		return factory == null ? Verifier.AOK : factory.apply(target);
	}
}
