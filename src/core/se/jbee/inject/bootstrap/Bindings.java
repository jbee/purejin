/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.bootstrap.Metaclass.metaclass;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.jbee.inject.Array;
import se.jbee.inject.InconsistentBinding;
import se.jbee.inject.Type;

/**
 * {@link Bindings} accumulate the {@link Binding} 4-tuples.
 * 
 * Any builder is just a utility to construct calls to {@link #add(Binding)}
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bindings {

	public static Bindings bindings( Macros macros, Inspector inspector ) {
		return new Bindings( macros, inspector, new ArrayList<Binding<?>>( 128 ), false );
	}

	public final Macros macros;
	public final Inspector inspector;
	public final boolean autobinding;
	
	private final List<Binding<?>> bindings;

	private Bindings( Macros macros, Inspector inspector, List<Binding<?>> bindings,
			boolean autobinding ) {
		this.macros = macros;
		this.inspector = inspector;
		this.bindings = bindings;
		this.autobinding = autobinding;
	}

	public Bindings autobinding() {
		return new Bindings( macros, inspector, bindings, true );
	}

	public Bindings using( Inspector inspector ) {
		return new Bindings( macros, inspector, bindings, autobinding );
	}

	/**
	 * Add (accumulate) a binding described by the 4-tuple given.
	 */
	public <T> void add( Binding<T> binding ) {
		if (!binding.isComplete()) {
			throw new InconsistentBinding("Incomplete binding added: "+binding);
		}
		bindings.add( binding );
		if ( !autobinding ) {
			return;
		}
		//OPEN this can be extracted to a macro by introducing a Auto type a macro could be bound to
		for ( Type<? super T> supertype : binding.type().supertypes() ) {
			// Object is of cause a superclass of everything but not indented when doing auto-binds
			if ( supertype.rawType != Object.class ) {
				bindings.add( binding.typed( supertype ) );
			}
		}
	}

	public Binding<?>[] toArray() {
		return Array.of( bindings, Binding.class );
	}

	public Binding<?>[] declareFrom( Module... modules ) {
		Set<Class<?>> declared = new HashSet<Class<?>>();
		Set<Class<?>> multimodals = new HashSet<Class<?>>();
		for ( Module m : modules ) {
			Class<? extends Module> ns = m.getClass();
			final boolean hasBeenDeclared = declared.contains( ns );
			if ( hasBeenDeclared ) {
				if ( !metaclass( ns ).monomodal() ) {
					multimodals.add( ns );
				}
			}
			if ( !hasBeenDeclared || multimodals.contains( ns ) ) {
				m.declare( this );
				declared.add( ns );
			}
		}
		return toArray();
	}

}
