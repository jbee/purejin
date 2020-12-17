package se.jbee.inject.bootstrap;

import se.jbee.inject.*;
import se.jbee.inject.bind.*;
import se.jbee.inject.config.*;
import se.jbee.inject.defaults.DefaultBindingConsolidation;
import se.jbee.inject.defaults.DefaultValueBinders;
import se.jbee.inject.lang.Type;
import se.jbee.inject.lang.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;

import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.lang.Type.raw;

/**
 * The {@link Environment} is the map based implementation of an {@link Env}
 * that mainly exists to solve the hen-egg situation that originates from {@link
 * Env} at times themselves being bootstrapped on the basis of an {@link Env}.
 * <p>
 * It only allows to declare "global" properties using {@link #with(Type,
 * Object)} and others.
 */
public final class Environment implements Env {

	/**
	 * The most basic {@link Env} that is used as default to bootstrap {@link
	 * Injector} contexts from root {@link se.jbee.inject.bind.Bundle} (s) or
	 * even a bootstrapped a name-spaced {@link Env} itself.
	 */
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
			.with(ConstructsBy.class, ConstructsBy.OPTIMISTIC) //
			.with(SharesBy.class, SharesBy.noFields) //
			.with(ProducesBy.class, ProducesBy.noMethods) //
			.with(NamesBy.class, NamesBy.defaultName) //
			.with(ScopesBy.class, ScopesBy.alwaysDefault) //
			.with(HintsBy.class, HintsBy.noParameters) //
			.with(Annotated.Enhancer.class, Annotated.SOURCE) //
			.with(BindingConsolidation.class, DefaultBindingConsolidation::consolidate) //
			.with(Env.GP_USE_DEEP_REFLECTION, false) //
			.with(Env.GP_DEEP_REFLECTION_PACKAGES, Packages.class, Packages.ALL) //
			.with(Env.GP_USE_VERIFICATION,false) //
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

	/**
	 * @param completed some {@link Env}
	 * @return This {@link Env} added as a fallback to the provided {@link Env}
	 */
	public Environment complete(Env completed) {
		return new Environment(readonly, values, false, completed);
	}

	@SuppressWarnings("unchecked")
	private HashMap<Instance<?>, Object> copyOfValues() {
		return (HashMap<Instance<?>, Object>) values.clone();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T property(Name qualifier, Type<T> property, Package ns) {
		if (decorated != null && !override) {
			try {
				return decorated.property(qualifier, property, ns);
			} catch (InconsistentDeclaration e) {
				// fall through and complement...
			}
		}
		Instance<T> key = instance(qualifier, property);
		Object value = values.get(key);
		if (value != null || values.containsKey(key))
			return (T) value;
		if (decorated != null && override)
			return decorated.property(qualifier, property, ns);
		throw InconsistentBinding.undefinedEnvProperty(qualifier, property, ns);
	}

	public Environment with(String qualifier, boolean value) {
		return with(qualifier, boolean.class, value);
	}

	public <T> Environment with(Class<T> globalProperty, T value) {
		return with(raw(globalProperty), value);
	}

	public <T> Environment with(Type<T> globalProperty, T value) {
		return with(Name.DEFAULT.toString(), globalProperty, value);
	}

	public <T> Environment with(String qualifier, Class<T> globalProperty, T value) {
		return with(qualifier, raw(globalProperty), value);
	}

	public <T> Environment with(String qualifier, Type<T> globalProperty, T value) {
		if (readonly) {
			return new Environment(false, copyOfValues(), override, decorated) //
					.with(qualifier, globalProperty, value);
		}
		values.put(instance(named(qualifier), globalProperty), value);
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

	public <A extends Annotation> Environment withTypePattern(Class<A> qualifier,
			ModuleWith<Class<?>> value) {
		//TODO check target type matches
		return with(named(qualifier).toString(), ModuleWith.TYPE_ANNOTATION, value);
	}

	public <A extends Annotation> Environment withMethodPattern(Class<A> qualifier,
			ModuleWith<Method> value) {
		//TODO check target type matches
		return with(named(qualifier).toString(), ModuleWith.METHOD_ANNOTATION, value);
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
