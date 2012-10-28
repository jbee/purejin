/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject;

/**
 * A VO to answer: Where does a bind came from ? Was is implicit or explicit ?
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

	public boolean isExplicit() {
		return declarationType == DeclarationType.EXPLICIT;
	}

	@Override
	public String toString() {
		String i = ident.toString();
		if ( ! ( ident instanceof Class<?> ) ) {
			i = ident.getClass().getCanonicalName() + ":" + i;
		}
		return isExplicit()
			? i
			: "(" + i + ")";
	}

	@Override
	public boolean morePreciseThan( Source other ) {
		return declarationType.morePreciseThan( other.declarationType );
	}

	public boolean isImplicit() {
		return declarationType == DeclarationType.IMPLICIT;
	}

	public Source typed( DeclarationType type ) {
		return declarationType == type
			? this
			: new Source( ident, type );
	}
}
