/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bootstrap;

import static se.jbee.inject.util.Metaclass.metaclass;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.jbee.inject.Array;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;
import se.jbee.inject.Type;

/**
 * {@link Bindings} accumulate the {@link Binding} 4-tuples.
 * 
 * Any builder is just a utility to construct calls to
 * {@link #add(Resource, Supplier, Scope, Source)}
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bindings {

	public static Bindings bindings( Inspector inspector ) {
		return new Bindings( inspector, new ArrayList<Binding<?>>( 100 ), false );
	}

	//TODO by making this data we lost ability to just 'visit' expanded bindings - the List could be replace by a custom interface to regain this ability.

	private final Inspector inspector;
	private final List<Binding<?>> bindings;
	private final boolean autobinding;

	private Bindings( Inspector inspector, List<Binding<?>> bindings, boolean autobinding ) {
		super();
		this.inspector = inspector;
		this.bindings = bindings;
		this.autobinding = autobinding;
	}

	public Bindings autobinding() {
		return new Bindings( inspector, bindings, true );
	}

	public Bindings using( Inspector inspector ) {
		return new Bindings( inspector, bindings, autobinding );
	}

	/**
	 * @return the chosen strategy to pick the {@link Constructor}s or {@link Method}s used to
	 *         create instances.
	 */
	public Inspector getInspector() {
		return inspector;
	}

	/**
	 * Add (accumulate) a binding described by the 4-tuple given.
	 * 
	 * @param resource
	 *            describes the identity of the resulting instance(s)
	 * @param supplier
	 *            creates this instance(s)
	 * @param scope
	 *            describes and controls the life-cycle of the instance(s)
	 * @param source
	 *            describes the origin of the binding (this call) and its meaning
	 */
	public <T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
			Source source ) {
		addInternal( resource, supplier, scope, source );
		if ( !autobinding ) {
			return;
		}
		Type<T> type = resource.getType();
		for ( Type<? super T> supertype : type.supertypes() ) {
			// Object is of cause a superclass of everything but not indented when doing auto-binds
			if ( supertype.getRawType() != Object.class ) {
				addInternal( resource.typed( supertype ), supplier, scope, source );
			}
		}
	}

	private <T> void addInternal( Resource<T> resource, Supplier<? extends T> supplier,
			Scope scope, Source source ) {
		bindings.add( new Binding<T>( resource, supplier, scope, source ) );
	}

	public Binding<?>[] toArray() {
		return Array.of( bindings, Binding.class );
	}

	public static Binding<?>[] expand( Inspector inspector, Module... modules ) {
		Set<Class<?>> declared = new HashSet<Class<?>>();
		Set<Class<?>> multimodals = new HashSet<Class<?>>();
		Bindings bindings = bindings( inspector );
		for ( Module m : modules ) {
			Class<? extends Module> ns = m.getClass();
			final boolean hasBeenDeclared = declared.contains( ns );
			if ( hasBeenDeclared ) {
				if ( !metaclass( ns ).monomodal() ) {
					multimodals.add( ns );
				}
			}
			if ( !hasBeenDeclared || multimodals.contains( ns ) ) {
				m.declare( bindings );
				declared.add( ns );
			}
		}
		return bindings.toArray();
	}

}
