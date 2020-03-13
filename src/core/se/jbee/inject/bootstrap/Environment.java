package se.jbee.inject.bootstrap;

import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map.Entry;

import se.jbee.inject.Env;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.Type;
import se.jbee.inject.Utils;
import se.jbee.inject.declare.InconsistentBinding;
import se.jbee.inject.declare.ModuleWith;
import se.jbee.inject.declare.ValueBinder;

public final class Environment implements Env {

	public static Environment override(Env overridden) {
		return new Environment(false, new HashMap<Instance<?>, Object>(), true,
				overridden);
	}

	public static Environment complete(Env completed) {
		return new Environment(false, new HashMap<Instance<?>, Object>(), false,
				completed);
	}

	private final boolean readonly;
	private final HashMap<Instance<?>, Object> values;
	private final boolean override;
	private final Env decorated;

	public Environment() {
		this(false, new HashMap<>(), true, null);
	}

	private Environment(boolean sealed, HashMap<Instance<?>, Object> values,
			boolean override, Env decorated) {
		this.readonly = sealed;
		this.values = values;
		this.override = override;
		this.decorated = decorated;
	}

	public Environment readonly() {
		return new Environment(true, values, override, decorated);
	}

	@SuppressWarnings("unchecked")
	private HashMap<Instance<?>, Object> copyOfValues() {
		return (HashMap<Instance<?>, Object>) values.clone();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T property(Name name, Type<T> property, Package pkg) {
		if (decorated != null && !override) {
			try {
				return decorated.property(name, property, pkg);
			} catch (InconsistentBinding e) {
				// fall through and complement...
			}
		}
		Instance<T> key = instance(name, property);
		Object value = values.get(key);
		if (value != null || values.containsKey(key))
			return (T) value;
		if (decorated != null && override)
			return decorated.property(name, property, pkg);
		throw InconsistentBinding.undefinedEnvProperty(name, property, pkg);
	}

	public <T> Environment with(Class<T> property, T value) {
		return with(raw(property), value);
	}

	public <T> Environment with(Type<T> property, T value) {
		return with(Name.DEFAULT.toString(), property, value);
	}

	public <T> Environment with(String name, Class<T> property, T value) {
		return with(name, raw(property), value);
	}

	public <T> Environment with(String name, Type<T> property, T value) {
		if (readonly) {
			return new Environment(false, copyOfValues(), override, decorated) //
					.with(name, property, value);
		}
		System.out.println(instance(named(name), property) + " => " + value);
		values.put(instance(named(name), property), value);
		return this;
	}

	public <T> Environment withMacro(Class<? extends ValueBinder<T>> value) {
		return withBinder(Utils.instance(value));
	}

	public <T> Environment withBinder(ValueBinder<T> value) {
		@SuppressWarnings("unchecked")
		Type<ValueBinder<T>> type = (Type<ValueBinder<T>>) Type.supertype(ValueBinder.class,
				Type.raw(value.getClass()));
		return with(type, value);
	}

	public <A extends Annotation> Environment withAnnotation(Class<A> name,
			ModuleWith<Class<?>> value) {
		return with(named(name).toString(),
				raw(ModuleWith.class).parametized(Type.CLASS), value);
	}

	@SafeVarargs
	public final <F extends Enum<F>> Environment withToggled(Class<F> flags,
			F... enabled) {
		Environment res = this;
		for (F flag : enabled) {
			res = flag == null
				? res.with("null", flags, null)
				: res.with(flag.name(), raw(flags), flag);
		}
		return res;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("{\n");
		for (Entry<Instance<?>, Object> e : values.entrySet()) {
			str.append('\t').append(e.getKey()).append(" => ").append(
					e.getValue()).append('\n');
		}
		str.append('}');
		return str.toString();
	}
}
