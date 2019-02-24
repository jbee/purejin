/*
 *  Copyright (c) 2012-2017, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.Array.array;
import static se.jbee.inject.bootstrap.Metaclass.metaclass;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.jbee.inject.InconsistentBinding;

/**
 * {@link Bindings} accumulate the {@link Binding} 4-tuples.
 *
 * Any builder is just a utility to construct calls to {@link #add(Binding)}
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bindings {

	public static Bindings bindings(Macros macros, Inspector inspector) {
		return new Bindings(macros, inspector, new ArrayList<>(128));
	}

	public final Macros macros;
	public final Inspector inspector;

	private final List<Binding<?>> bindings;

	private Bindings(Macros macros, Inspector inspector,
			List<Binding<?>> bindings) {
		this.macros = macros;
		this.inspector = inspector;
		this.bindings = bindings;
	}

	public Bindings using(Inspector inspector) {
		return new Bindings(macros, inspector, bindings);
	}

	/**
	 * Add (accumulate) a binding described by the 4-tuple given.
	 */
	public <T> void add(Binding<T> complete) {
		if (!complete.isComplete()) {
			throw new InconsistentBinding(
					"Incomplete binding added: " + complete);
		}
		// NB. #64 here we can inform post binding that about the new binding
		bindings.add(complete);
	}

	public void expandInto(Binding<?> binding) {
		macros.expandInto(this, binding, binding);
	}

	public Binding<?>[] toArray() {
		return array(bindings, Binding.class);
	}

	public Binding<?>[] declareFrom(Module... modules) {
		Set<Class<?>> declared = new HashSet<>();
		Set<Class<?>> multimodals = new HashSet<>();
		for (Module m : modules) {
			Class<? extends Module> ns = m.getClass();
			final boolean hasBeenDeclared = declared.contains(ns);
			if (hasBeenDeclared) {
				if (!metaclass(ns).monomodal()) {
					multimodals.add(ns);
				}
			}
			if (!hasBeenDeclared || multimodals.contains(ns)) {
				m.declare(this);
				declared.add(ns);
			}
		}
		return toArray();
	}

}
