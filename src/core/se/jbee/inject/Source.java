/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * Where does a bind came from and what type of declaration has it been.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Source
		implements PreciserThan<Source> {

	public static Source source( Object module ) {
		return new Source( module, DeclarationType.EXPLICIT );
	}

	private final Object ident;
	private final DeclarationType declarationType;

	private Source( Object ident, DeclarationType declarationType ) {
		super();
		this.ident = ident;
		this.declarationType = declarationType;
	}

	public Object getIdent() {
		return ident;
	}

	public DeclarationType getType() {
		return declarationType;
	}

	@Override
	public String toString() {
		String id = ident.toString();
		if ( ! ( ident instanceof Class<?> ) ) {
			id = ident.getClass().getCanonicalName() + ":" + id;
		}
		return declarationType == DeclarationType.IMPLICIT
			? "(" + id + ")"
			: id;
	}

	@Override
	public boolean morePreciseThan( Source other ) {
		return declarationType.morePreciseThan( other.declarationType );
	}

	public Source typed( DeclarationType type ) {
		return declarationType == type
			? this
			: new Source( ident, type );
	}
}
