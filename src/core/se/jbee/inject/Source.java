/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * Where does a bind come from and what type of declaration has it been.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Source
		implements PreciserThan<Source> {

	public static Source source( Class<?> module ) {
		return new Source( module, DeclarationType.EXPLICIT );
	}

	private final Class<?> ident;
	private final DeclarationType declarationType;

	private Source( Class<?> ident, DeclarationType declarationType ) {
		super();
		this.ident = ident;
		this.declarationType = declarationType;
	}

	public Class<?> getIdent() {
		return ident;
	}

	public DeclarationType getType() {
		return declarationType;
	}

	@Override
	public String toString() {
		return declarationType.name() + " " + ident.getCanonicalName();
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
