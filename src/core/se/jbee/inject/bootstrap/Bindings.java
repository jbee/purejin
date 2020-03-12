/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Utils.arrayOf;
import static se.jbee.inject.Utils.isClassMonomodal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.jbee.inject.Instance;
import se.jbee.inject.Locator;
import se.jbee.inject.Name;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.Type;
import se.jbee.inject.config.Annotations;
import se.jbee.inject.config.Env;
import se.jbee.inject.config.Mirrors;
import se.jbee.inject.declare.Macro;
import se.jbee.inject.declare.Module;
import se.jbee.inject.declare.ModuleWith;

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

	@Deprecated
	public Bindings with(Mirrors mirrors) {
		return new Bindings(list);
	}

	@Deprecated
	public Bindings with(Macros macros) {
		return new Bindings(list);
	}

	@Deprecated
	public Bindings with(Annotations annotations) {
		return new Bindings(list);
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

	//TODO move out of here?
	public void addExpanded(Env env, Binding<?> binding) {
		addExpanded(env, binding, binding);
	}

	public <V> void addExpanded(Env env, Binding<?> binding, V value) {
		@SuppressWarnings("unchecked")
		Class<V> type = (Class<V>) value.getClass();
		Macro<V> macro = env.ifBound(type, binding.source.ident.getPackage());
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
			ModuleWith<Class<?>> then = env.annotationProperty(a.annotationType(),
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
		addExpanded(env,
				Binding.binding(new Locator<>(instance), BindingType.PREDEFINED,
						Supply.constant(constant), Scope.container, source));
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

}
