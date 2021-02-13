package se.jbee.inject;

import se.jbee.lang.Cast;
import se.jbee.lang.Type;
import se.jbee.lang.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.DEFAULT;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;

/**
 * An {@link Env} is a like a key-value property map where the keys are {@link
 * Type}s and the values instances of the key type.
 * <p>
 * To distinguish multiple values of the same {@link Type} a {@link Name}
 * qualifier can be added added.
 * <p>
 * To avoid collisions between qualified properties as used within different
 * software modules properties are usually defined local to the defining
 * software module. During lookup the {@link Class} of the processed module
 * is used as the name-space key.
 * <p>
 * Properties that are considered namespace specific are resolved providing the
 * {@link Class} of the defining module or "bean".
 * <p>
 * Properties that are considered "globally" used do not use a particular
 * namespace ({@code null}).
 */
@FunctionalInterface
public interface Env {

	/**
	 * Property name used to configure a boolean if {@link Verifier}s are tried
	 * to be resolved during the binding process. If not, {@link Verifier#AOK}
	 * (no validation) is used (default).
	 */
	String USE_VERIFICATION = "verify";

	/**
	 * Boolean flag property which when set to {@code true} adds the bindings
	 * that were the basis of an {@link Injector} or {@link Env} context as
	 * a {@link Resource} in the context that can be resolved as array type of
	 * the binding class used.
	 */
	String BIND_BINDINGS = "self-bind";

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

			private final Env env;
			private final Class<?> ns;

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

	/**
	 * @return An {@link Env} which isolates previous contributions using {@code
	 * with} from future ones. In other words the set of added properties added
	 * so far is not be changed by future {@code with} calls.
	 */
	default Env withIsolate() {
		return this;
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

			private final Env env;
			private final Map<Instance<?>, Object> values;
			private boolean isolate = false;

			EnvWith(Env env, Map<Instance<?>, Object> values) {
				this.env = env;
				this.values = values;
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
			public Env withIsolate() {
				isolate = true;
				return this;
			}

			@Override
			public <T> Env with(Name qualifier, Type<T> property, T value) {
				if (isolate)
					return new EnvWith(this, new HashMap<>()).with(qualifier, property, value);
				values.put(instance(qualifier, property), value);
				return this;
			}

			@Override
			public Env in(Class<?> ns) {
				Env in = env.in(ns);
				return in == env ? this : new EnvWith(in, values);
			}

			@Override
			public String toString() {
				StringBuilder str = new StringBuilder();
				str.append("EnvWith[");
				for (Map.Entry<Instance<?>, Object> e : values.entrySet()) {
					str.append("\n  ").append(e.getKey().type.simpleName());
					if (!e.getKey().name.isDefault())
						str.append(" \"").append(e.getKey().name).append('"');
					str.append(" = ").append(e.getValue());
				}
				str.append("\n]\n").append(env);
				return str.toString();
			}
		}
		return new EnvWith(this, new HashMap<>()).with(qualifier, property, value);
	}

	@SuppressWarnings("unchecked")
	default <E extends Enum<E>> Env withDependent(Class<E> set, E... elements) {
		Env res = this;
		for (E e : elements) {
			res = e == null
					? res.with("null", set, null)
					: res.with(e.name(), raw(set), e);
		}
		return res;
	}

	default <E extends Enum<E>> boolean isInstalled(Class<E> dependentOn, E element) {
		return element == null
				? property("null", dependentOn, dependentOn.getEnumConstants()[0]) == null
				: property(element.name(), dependentOn,null) != null;
	}

	default <T> Verifier verifierFor(T target) {
		if (!property(Env.USE_VERIFICATION, false))
			return Verifier.AOK;
		@SuppressWarnings("unchecked")
		Class<T> targetClass = (Class<T>) target.getClass();
		Function<T, Verifier> factory = property(Cast.functionTypeOf(
				targetClass, Verifier.class), null);
		return factory == null ? Verifier.AOK : factory.apply(target);
	}
}
