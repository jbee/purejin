package de.jbee.inject;

/**
 * A VO to answer: Where does a bind came from ? Was is implicit or explicit ?
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Source {

	public static Source source( Class<? extends Module> module ) {
		return new Source( module, true );
	}

	private final Class<? extends Module> module;
	private final boolean explicit;

	private Source( Class<? extends Module> module, boolean explicit ) {
		super();
		this.module = module;
		this.explicit = explicit;
	}

	public Class<? extends Module> getModule() {
		return module;
	}

	public boolean isExplicit() {
		return explicit;
	}

	public Source implicit() {
		return new Source( module, false );
	}

	@Override
	public String toString() {
		return explicit
			? module.getCanonicalName()
			: "(" + module.getCanonicalName() + ")";
	}
}
