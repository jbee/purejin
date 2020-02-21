/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Utils.arrayOf;
import static se.jbee.inject.Utils.isClassMonomodal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.jbee.inject.config.Mirrors;

/**
 * {@link Bindings} accumulate the {@link Binding} 4-tuples.
 *
 * Any builder is just a utility to construct calls to {@link #add(Binding)}
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bindings {

	public static Bindings newBindings() {
		return new Bindings(new ArrayList<>(128), Macros.DEFAULT,
				Mirrors.DEFAULT);
	}

	private final List<Binding<?>> list;
	public final Macros macros;
	public final Mirrors mirrors;

	private Bindings(List<Binding<?>> list, Macros macros, Mirrors mirrors) {
		this.list = list;
		this.macros = macros;
		this.mirrors = mirrors;
	}

	public Bindings with(Mirrors mirrors) {
		return new Bindings(list, macros, mirrors);
	}

	public Bindings with(Macros macros) {
		return new Bindings(list, macros, mirrors);
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

	public void addExpanded(Binding<?> binding) {
		macros.expandInto(this, binding, binding);
	}

	public Binding<?>[] toArray() {
		return arrayOf(list, Binding.class);
	}

	public Binding<?>[] declareFrom(Module... modules) {
		Set<Class<?>> declared = new HashSet<>();
		Set<Class<?>> multimodals = new HashSet<>();
		for (Module m : modules) {
			Class<? extends Module> ns = m.getClass();
			final boolean hasBeenDeclared = declared.contains(ns);
			if (hasBeenDeclared && !isClassMonomodal(ns))
				multimodals.add(ns);
			if (!hasBeenDeclared || multimodals.contains(ns)) {
				m.declare(this);
				declared.add(ns);
			}
		}
		return toArray();
	}

}
