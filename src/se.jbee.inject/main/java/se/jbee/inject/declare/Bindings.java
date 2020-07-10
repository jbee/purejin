/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.declare;

import static se.jbee.inject.Type.raw;
import static se.jbee.inject.Utils.arrayOf;
import static se.jbee.inject.Utils.isClassMonomodal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.jbee.inject.Dependency;
import se.jbee.inject.Env;
import se.jbee.inject.Generator;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Locator;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.Scoping;
import se.jbee.inject.Source;
import se.jbee.inject.Type;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.container.Supplier;

/**
 * {@link Bindings} accumulate the {@link Binding} 4-tuples.
 *
 * Any builder is just a utility to construct calls to {@link #add(Binding)}
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bindings {

	public static Bindings newBindings() {
		return new Bindings(new ArrayList<>(128));
	}

	private final List<Binding<?>> list;

	private Bindings(List<Binding<?>> list) {
		this.list = list;
	}

	/**
	 * Add (accumulate) a binding described by the 4-tuple given.
	 */
	public <T> void add(Binding<T> complete) {
		if (!complete.isComplete())
			throw InconsistentBinding.addingIncomplete(complete);
		// NB. #64 here we can inform post binding that about the new binding
		list.add(complete);
	}

	public void addExpanded(Env env, Binding<?> binding) {
		addExpanded(env, binding, binding);
	}

	public <V> void addExpanded(Env env, Binding<?> binding, V value) {
		@SuppressWarnings("unchecked")
		Class<V> type = (Class<V>) value.getClass();
		@SuppressWarnings("unchecked")
		ValueBinder<V> macro = env.property(
				raw(ValueBinder.class).parametized(Type.classType(type)),
				binding.source.ident.getPackage());
		if (macro == null)
			throw InconsistentBinding.undefinedMacroType(binding, type);
		macro.expand(env, value, binding, this);
	}

	//TODO move out of here?
	public void addAnnotated(Env env, Class<?> annotated) {
		Annotation[] as = annotated.getAnnotations();
		if (as.length == 0)
			throw InconsistentBinding.noTypeAnnotation(annotated);
		int n = 0;
		for (Annotation a : as) {
			@SuppressWarnings("unchecked")
			ModuleWith<Class<?>> then = env.property(
					Name.named(a.annotationType()),
					raw(ModuleWith.class).parametized(Type.CLASS),
					annotated.getPackage());
			//TODO add a meta annotation to mark annotation that are expected to be defined
			// if such an annotation is present but no effect defined it is an binding error
			if (then != null) {
				then.declare(this, env, annotated);
				n++;
			}
		}
		if (n == 0)
			throw InconsistentBinding.noTypeAnnotation(annotated);
	}

	public <T> void addConstant(Env env, Source source, Name name,
			Class<T> type, T constant) {
		addConstant(env, source, Instance.instance(name, Type.raw(type)),
				constant);
	}

	public <T> void addConstant(Env env, Source source, Instance<T> instance,
			T constant) {
		//TODO maybe only use Scope.container if scope is not set to application explicitly since container also bypasses postConstruct?
		addExpanded(env,
				Binding.binding(new Locator<>(instance), BindingType.PREDEFINED,
						supplyConstant(constant), Scope.container, source));
	}

	public Binding<?>[] toArray() {
		return arrayOf(list, Binding.class);
	}

	public Binding<?>[] declaredFrom(Env env, Module... modules) {
		declareFrom(env, modules);
		return toArray();
	}

	public void declareFrom(Env env, Module... modules) {
		Set<Class<?>> declared = new HashSet<>();
		Set<Class<?>> multimodals = new HashSet<>();
		for (Module m : modules) {
			Class<? extends Module> ns = m.getClass();
			final boolean hasBeenDeclared = declared.contains(ns);
			if (hasBeenDeclared && !isClassMonomodal(ns))
				multimodals.add(ns);
			if (!hasBeenDeclared || multimodals.contains(ns)) {
				m.declare(this, env);
				declared.add(ns);
			}
		}
	}

	public static <T> Supplier<T> supplyConstant(T constant) {
		return new ConstantSupplier<>(constant);
	}

	/**
	 * In contrast to {@link #supplyConstant(Object)} which does supply the
	 * constant as {@link Generator} and thereby does not support custom
	 * {@link Scoping} or {@link Scope}s this way of supplying the constant will
	 * treat the constant as bean, that is like any "dynamically" supplied
	 * value.
	 * 
	 * @param <T> type of the constant
	 * @param constant a bean that requires {@link Scoping} effects.
	 * @return A {@link Supplier} that supplies the given constant with
	 *         {@link Scoping} effects.
	 */
	public static <T> Supplier<T> supplyScopedConstant(T constant) {
		return (dep, context) -> constant;
	}

	/**
	 * Since the {@link Supplier} also implements {@link Generator} it is used
	 * directly without any {@link Scoping} effects. Effectively a constant
	 * always has the {@link Scope#container}.
	 * 
	 * The implementation also implements {@link #equals(Object)} and
	 * {@link #hashCode()} to allow elimination of duplicate constant bindings.
	 */
	private static final class ConstantSupplier<T>
			implements Supplier<T>, Generator<T> {

		private final T constant;

		ConstantSupplier(T constant) {
			this.constant = constant;
		}

		@Override
		public T generate(Dependency<? super T> dep)
				throws UnresolvableDependency {
			return constant;
		}

		@Override
		public T supply(Dependency<? super T> dep, Injector context)
				throws UnresolvableDependency {
			return generate(dep);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof ConstantSupplier
				&& constant == ((ConstantSupplier<?>) obj).constant;
		}

		@Override
		public int hashCode() {
			return constant == null ? super.hashCode() : constant.hashCode();
		}

		@Override
		public String toString() {
			return "constant " + String.valueOf(constant);
		}
	}
}
