/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.*;
import se.jbee.inject.Annotated.Enhancer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.classType;
import static se.jbee.lang.Type.raw;
import static se.jbee.lang.Utils.arrayOf;
import static se.jbee.lang.Utils.isClassConceptStateless;

/**
 * {@link Bindings} accumulate the {@link Binding} during the bootstrapping.
 *
 * Any builder is just a utility to construct calls to
 * {@link #add(Env, Binding)}
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
	public <T> void add(Env env, Binding<T> complete) {
		if (!complete.isComplete())
			throw InconsistentBinding.addingIncomplete(complete);
		Enhancer enhancer = env.property(Enhancer.class, Annotated.SOURCE);
		list.add(complete.annotatedBy(enhancer.apply(complete.annotations)));
	}

	public void addExpanded(Env env, Binding<?> binding) {
		addExpanded(env, binding, binding);
	}

	public <V extends Descriptor> void addExpanded(Env env, Binding<?> binding, V value) {
		@SuppressWarnings("unchecked")
		Class<V> type = (Class<V>) value.getClass();
		@SuppressWarnings("unchecked")
		ValueBinder<V> binder = env.property(
				raw(ValueBinder.class).parameterized(classType(type)));
		if (binder == null)
			throw InconsistentBinding.undefinedValueBinderType(binding, type);
		binder.expand(env, value, binding, this);
	}

	public void addAnnotated(Env env, Class<?> annotated) {
		Annotation[] as = annotated.getAnnotations();
		int n = 0;
		//TODO add a meta annotation to mark annotations that are expected to be defined
		// if such an annotation is present but no effect defined it is a binding error
		// should be doable by just defining a module that tries to resolve the annotations from the environment => confirm with a test
		for (Annotation a : as)
			if (addsAnnotatedType(env, annotated, a))
				n++;
		for (Method m : annotated.getMethods())
			for (Annotation a : m.getDeclaredAnnotations())
				if (addsAnnotatedMethod(env, m, a))
					n++;
		if (n == 0)
			throw InconsistentBinding.noAnnotationModule(annotated);
	}

	private boolean addsAnnotatedType(Env env, Class<?> annotated, Annotation annotation) {
		ModuleWith<Class<?>> then = env.in(annotated).property(
				named(annotation.annotationType()).toString(),
				ModuleWith.TYPE_ANNOTATION, null);
		if (then == null)
			return false;
		then.declare(this, env, annotated);
		return true;
	}

	private boolean addsAnnotatedMethod(Env env, Method annotated, Annotation annotation) {
		ModuleWith<Method> then = env.in(annotated.getDeclaringClass()).property(
				named(annotation.annotationType()).toString(),
				ModuleWith.METHOD_ANNOTATION, null);
		if (then == null)
			return false;
		then.declare(this, env, annotated);
		return true;
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
		Set<Class<?>> stateful = new HashSet<>();
		for (Module m : modules) {
			Class<? extends Module> ns = m.getClass();
			final boolean hasBeenDeclared = declared.contains(ns);
			if (hasBeenDeclared && !isClassConceptStateless(ns))
				stateful.add(ns);
			if (!hasBeenDeclared || stateful.contains(ns)) {
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
	 * {@link ScopeLifeCycle} or {@link Scope}s this way of supplying the
	 * constant will treat the constant as bean, that is like any "dynamically"
	 * supplied value.
	 *
	 * @param <T> type of the constant
	 * @param constant a bean that requires {@link ScopeLifeCycle} effects.
	 * @return A {@link Supplier} that supplies the given constant with
	 *         {@link ScopeLifeCycle} effects.
	 */
	public static <T> Supplier<T> supplyScopedConstant(T constant) {
		return (dep, context) -> constant;
	}

	/**
	 * Since the {@link Supplier} also implements {@link Generator} it is used
	 * directly without any {@link ScopeLifeCycle} effects. Effectively a
	 * constant always has the {@link Scope#container}.
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
			return "constant " + constant;
		}
	}
}
