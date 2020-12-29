package se.jbee.inject;

import se.jbee.inject.lang.Cast;
import se.jbee.inject.lang.Reflect;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.DEFAULT;
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

	default <T> T property(String qualifier, Type<T> property) {
		return property(named(qualifier), property, null);
	}

	default <T> T property(String qualifier, Class<T> property, T defaultValue) {
		return property(qualifier, raw(property), defaultValue);
	}

	default <T> T property(String qualifier, Type<T> property, T defaultValue) {
		return Utils.orElse(defaultValue,
				() ->  property(named(qualifier), property, null));
	}

	default Env in(Class<?> ns) {
		final class EnvIn implements Env {

			final Env env;
			final Class<?> ns;

			EnvIn(Env env, Class<?> ns) {
				this.env = env;
				this.ns = ns;
			}

			@Override
			public Env in(Class<?> ns) {
				return ns == null ? env : ns == this.ns ? this : new EnvIn(env, ns);
			}

			@Override
			public <T> T property(Name qualifier, Type<T> property, Class<?> ns) {
				return env.property(qualifier, property, this.ns);
			}

			@Override
			public String toString() {
				return "EnvIn[" + ns.getName() + "]\n" + env.toString();
			}
		}
		return ns == null ? this : new EnvIn(this, ns);
	}

	default Env with(String qualifier, boolean value) {
		return with(qualifier, boolean.class, value);
	}

	default <V> Env with(Class<V> property, V value) {
		return with(raw(property), value);
	}

	default <V> Env with(Type<V> property, V value) {
		return with(DEFAULT, property, value);
	}

	default <V> Env with(String qualifier, Class<V> property, V value) {
		return with(named(qualifier), raw(property), value);
	}

	default <V> Env with(String qualifier, Type<V> property, V value) {
		return with(named(qualifier), property, value);
	}

	default <V> Env with(Name qualifier, Type<V> property, V value) {
		/*
		 * Adds an overriding map of "global" values.
		 * This is mostly used to tweak the bootstrapping.
		 */
		final class EnvWith implements Env {

			final Env env;
			final Map<Instance<?>, Object> values = new HashMap<>();

			EnvWith(Env env) {
				this.env = env;
			}

			@SuppressWarnings("unchecked")
			@Override
			public <T> T property(Name qualifier, Type<T> property, Class<?> ns)
					throws InconsistentDeclaration {
				Instance<T> key = instance(qualifier, property);
				if (values.containsKey(key))
					return (T) values.get(key);
				return env.property(qualifier, property, ns);
			}

			@Override
			public <T> Env with(Name qualifier, Type<T> property, T value) {
				values.put(instance(qualifier, property), value);
				return this;
			}

			@Override
			public String toString() {
				return "EnvWith{" + "env=" + env + ", values=" + values + '}';
			}
		}
		return new EnvWith(this).with(qualifier, property, value);
	}

	default <F extends Enum<F>> Env withToggled(Class<F> flags, F... enabled) {
		Env res = this;
		for (F flag : enabled) {
			res = flag == null
					? res.with("null", flags, null)
					: res.with(flag.name(), raw(flags), flag);
		}
		return res;
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
