/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.DeclarationType;
import se.jbee.inject.Instance;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.Source;
import se.jbee.inject.Supplier;
import se.jbee.inject.Target;
import se.jbee.inject.Type;
import se.jbee.inject.bootstrap.Bindings;
import se.jbee.inject.bootstrap.Inspector;

/**
 * The data and behavior used to create binds.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Bind {

	public static Bindings autobinding( Bindings delegate ) {
		return new AutobindBindings( delegate );
	}

	public static Bind create( Bindings bindings, Inspector inspector, Source source, Scope scope ) {
		return new Bind( bindings, inspector, source, scope, Target.ANY );
	}

	final Bindings bindings;
	final Inspector inspector;
	final Source source;
	final Scope scope;
	final Target target;

	private Bind( Bindings bindings, Inspector inspector, Source source, Scope scope, Target target ) {
		super();
		this.bindings = bindings;
		this.inspector = inspector;
		this.source = source;
		this.scope = scope;
		this.target = target;
	}

	public Bind asMulti() {
		return as( DeclarationType.MULTI );
	}

	public Bind asAuto() {
		return as( DeclarationType.AUTO );
	}

	public Bind asImplicit() {
		return as( DeclarationType.IMPLICIT );
	}

	public Bind asDefault() {
		return as( DeclarationType.DEFAULT );
	}

	public Bind asRequired() {
		return as( DeclarationType.REQUIRED );
	}

	public Bind asProvided() {
		return as( DeclarationType.PROVIDED );
	}

	public Bind as( DeclarationType type ) {
		return with( source.typed( type ) );
	}

	public Bind using( Inspector inspector ) {
		return new Bind( bindings, inspector, source, scope, target );
	}

	public Bind per( Scope scope ) {
		return new Bind( bindings, inspector, source, scope, target );
	}

	public Bind with( Target target ) {
		return new Bind( bindings, inspector, source, scope, target );
	}

	public Bind into( Bindings bindings ) {
		return new Bind( bindings, inspector, source, scope, target );
	}

	public Bind autobinding() {
		return into( autobinding( bindings ) );
	}

	public Bind with( Source source ) {
		return new Bind( bindings, inspector, source, scope, target );
	}

	public Bind within( Instance<?> parent ) {
		return new Bind( bindings, inspector, source, scope, target.within( parent ) );
	}

	private static class AutobindBindings
			implements Bindings {

		private final Bindings delegate;

		AutobindBindings( Bindings delegate ) {
			super();
			this.delegate = delegate;
		}

		@Override
		public <T> void add( Resource<T> resource, Supplier<? extends T> supplier, Scope scope,
				Source source ) {
			delegate.add( resource, supplier, scope, source );
			Type<T> type = resource.getType();
			for ( Type<? super T> supertype : type.supertypes() ) {
				// Object is of cause a superclass of everything but not indented when doing auto-binds
				if ( supertype.getRawType() != Object.class ) {
					delegate.add( resource.typed( supertype ), supplier, scope, source );
				}
			}
		}
	}

}
