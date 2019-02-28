/*
 *  Copyright (c) 2012-2019, Jan Bernitt
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
import se.jbee.inject.config.ConstructionMirror;
import se.jbee.inject.config.NamingMirror;
import se.jbee.inject.config.ParameterisationMirror;
import se.jbee.inject.config.ProductionMirror;

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
				ConstructionMirror.mostParams, NamingMirror.defaultName,
				ProductionMirror.noMethods, ParameterisationMirror.noParameters);
	}

	private final List<Binding<?>> bindings;
	public final Macros macros;
	public final ConstructionMirror construction;
	public final NamingMirror naming;
	public final ProductionMirror production;
	public final ParameterisationMirror parameterisation;

	private Bindings(List<Binding<?>> bindings, Macros macros,
			ConstructionMirror construction, NamingMirror naming,
			ProductionMirror production,
			ParameterisationMirror parameterisation) {
		this.bindings = bindings;
		this.macros = macros;
		this.construction = construction;
		this.naming = naming;
		this.production = production;
		this.parameterisation = parameterisation;
	}

	public Bindings with(ConstructionMirror mirror) {
		return new Bindings(bindings, macros, mirror, naming, production,
				parameterisation);
	}

	public Bindings with(NamingMirror mirror) {
		return new Bindings(bindings, macros, construction, mirror,
				production, parameterisation);
	}

	public Bindings with(ProductionMirror mirror) {
		return new Bindings(bindings, macros, construction, naming, mirror,
				parameterisation);
	}

	public Bindings with(ParameterisationMirror mirror) {
		return new Bindings(bindings, macros, construction, naming, production,
				mirror);
	}

	public Bindings with(Macros macros) {
		return new Bindings(bindings, macros, construction, naming, production,
				parameterisation);
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
