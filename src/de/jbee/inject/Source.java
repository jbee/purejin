package de.jbee.inject;

/**
 * A VO to answer: Where does a bind came from ? Was is implicit or explicit ?
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Source
		implements PreciserThan<Source> {

	public static Source source( Class<?> module ) {
		return new Source( module, DeclarationType.EXPLICIT );
	}

	private final Class<?> module;
	private final DeclarationType declarationType;

	private Source( Class<?> module, DeclarationType declarationType ) {
		super();
		this.module = module;
		this.declarationType = declarationType;
	}

	public Class<?> getModule() {
		return module;
	}

	public boolean isExplicit() {
		return declarationType == DeclarationType.EXPLICIT;
	}

	public Source implicit() {
		return new Source( module, DeclarationType.IMPLICIT );
	}

	@Override
	public String toString() {
		return isExplicit()
			? module.getCanonicalName()
			: "(" + module.getCanonicalName() + ")";
	}

	public Source multi() {
		return new Source( module, DeclarationType.MULTI );
	}

	@Override
	public boolean morePreciseThan( Source other ) {
		return declarationType.morePreciseThan( other.declarationType );
	}

	public boolean isImplicit() {
		return declarationType == DeclarationType.IMPLICIT;
	}
}
