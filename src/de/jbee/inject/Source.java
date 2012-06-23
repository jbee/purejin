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

	public boolean isExplicit() {
		return declarationType == DeclarationType.EXPLICIT;
	}

	public Source implicit() {
		return new Source( ident, DeclarationType.IMPLICIT );
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

	public Source multi() {
		return new Source( ident, DeclarationType.MULTI );
	}

	@Override
	public boolean morePreciseThan( Source other ) {
		return declarationType.morePreciseThan( other.declarationType );
	}

	public boolean isImplicit() {
		return declarationType == DeclarationType.IMPLICIT;
	}

	public Source asDefault() {
		return new Source( ident, DeclarationType.DEFAULT );
	}
}
