package se.jbee.inject.bootstrap;

import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.raw;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map.Entry;

import se.jbee.inject.*;
import se.jbee.inject.config.*;
import se.jbee.inject.defaults.DefaultValueBinders;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;
import se.jbee.inject.bind.InconsistentBinding;
import se.jbee.inject.bind.ModuleWith;
import se.jbee.inject.bind.ValueBinder;

public final class Environment implements Env {

	public static final Environment DEFAULT = new Environment() //
			.with(Edition.class, Edition.FULL) //
			.withBinder(DefaultValueBinders.SUPER_TYPES) //
			.withBinder(DefaultValueBinders.NEW) //
			.withBinder(DefaultValueBinders.CONSTANT) //
			.withBinder(DefaultValueBinders.PRODUCES) //
			.withBinder(DefaultValueBinders.SHARES) //
			.withBinder(DefaultValueBinders.INSTANCE_REF) //
			.withBinder(DefaultValueBinders.PARAMETRIZED_REF) //
			.withBinder(DefaultValueBinders.ARRAY) //
			.with(ConstructsBy.class, ConstructsBy.common) //
			.with(SharesBy.class, SharesBy.noFields) //
			.with(ProducesBy.class, ProducesBy.noMethods) //
			.with(NamesBy.class, NamesBy.defaultName) //
			.with(ScopesBy.class, ScopesBy.alwaysDefault) //
			.with(HintsBy.class, HintsBy.noParameters) //
			.with(Annotated.Merge.class, Annotated.NO_MERGE) //
			.with(Env.GP_USE_DEEP_REFLECTION, boolean.class, false) //
			.with(Env.GP_DEEP_REFLECTION_PACKAGES, Packages.class, Packages.ALL) //
			.with(Env.GP_USE_VERIFICATION, boolean.class, false) //
			.readonly();

	public static Environment override(Env overridden) {
		return new Environment(false, new HashMap<>(), true, overridden);
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

	public Environment complete(Env completed) {
		return new Environment(readonly, values, false, completed);
	}

	@SuppressWarnings("unchecked")
	private HashMap<Instance<?>, Object> copyOfValues() {
		return (HashMap<Instance<?>, Object>) values.clone();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T property(Name name, Type<T> property, Package scope) {
		if (decorated != null && !override) {
			try {
				return decorated.property(name, property, scope);
			} catch (InconsistentDeclaration e) {
				// fall through and complement...
			}
		}
		Instance<T> key = instance(name, property);
		Object value = values.get(key);
		if (value != null || values.containsKey(key))
			return (T) value;
		if (decorated != null && override)
			return decorated.property(name, property, scope);
		throw InconsistentBinding.undefinedEnvProperty(name, property, scope);
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
		values.put(instance(named(name), property), value);
		return this;
	}

	public <T> Environment withBinder(Class<? extends ValueBinder<T>> value) {
		return withBinder(Utils.instantiate(value, this::accessible,
				e -> new InconsistentDeclaration("Failed to create ValueBinder of type: " + value, e)));
	}

	public <T> Environment withBinder(ValueBinder<T> value) {
		@SuppressWarnings("unchecked")
		Type<ValueBinder<T>> type = (Type<ValueBinder<T>>) Type.supertype(
				ValueBinder.class, Type.raw(value.getClass()));
		return with(type, value);
	}

	public <A extends Annotation> Environment withAnnotation(Class<A> name,
			ModuleWith<Class<?>> value) {
		return with(named(name).toString(), ModuleWith.ANNOTATION, value);
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
