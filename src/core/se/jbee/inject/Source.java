/*
 *  Copyright (c) 2012-2017, Jan Bernitt
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
		implements MorePreciseThan<Source> {

	public static Source source( Class<?> module ) {
		return new Source( module, DeclarationType.EXPLICIT, 0, 0 );
	}

	public final Class<?> ident;
	public final DeclarationType declarationType;
	public final int declarationNo;

	/**
	 * Number of declarations from this source in total.
	 */
	private int totalDeclarations;

	private Source( Class<?> ident, DeclarationType declarationType, int declarationNo, int totalDeclarations ) {
		this.ident = ident;
		this.declarationType = declarationType;
		this.declarationNo = declarationNo;
		this.totalDeclarations = totalDeclarations;
	}

	@Override
	public String toString() {
		return ident.getSimpleName() + "#" + declarationNo + "[" + declarationType.name() + "]";
	}

	@Override
	public boolean morePreciseThan( Source other ) {
		return declarationType.morePreciseThan( other.declarationType );
	}

	public Source typed( DeclarationType type ) {
		return declarationType == type
			? this
			: new Source( ident, type, declarationNo, totalDeclarations );
	}

	public Source next() {
		totalDeclarations++;
		return declarationNo > 0
			? this
			: new Source( ident, declarationType, totalDeclarations, 0 );
	}
}
