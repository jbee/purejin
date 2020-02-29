package se.jbee.inject.config;

public final class Mirrors {

	//TODO can Mirrors be Options?

	public static final Mirrors DEFAULT = new Mirrors(ConstructionMirror.common,
			ProductionMirror.noMethods, NamingMirror.defaultName,
			ParameterisationMirror.noParameters, ScopingMirror.alwaysDefault);

	public final ConstructionMirror construction;
	public final ProductionMirror production;
	public final NamingMirror naming;
	public final ParameterisationMirror parameterisation;
	public final ScopingMirror scoping;

	private Mirrors(ConstructionMirror construction,
			ProductionMirror production, NamingMirror naming,
			ParameterisationMirror parameterisation, ScopingMirror scoping) {
		this.construction = construction;
		this.production = production;
		this.naming = naming;
		this.parameterisation = parameterisation;
		this.scoping = scoping;
	}

	public Mirrors scopeBy(ScopingMirror mirror) {
		return new Mirrors(construction, production, naming, parameterisation,
				mirror);
	}

	public Mirrors constructBy(ConstructionMirror mirror) {
		return new Mirrors(mirror, production, naming, parameterisation,
				scoping);
	}

	public Mirrors nameBy(NamingMirror mirror) {
		return new Mirrors(construction, production, mirror, parameterisation,
				scoping);
	}

	public Mirrors produceBy(ProductionMirror mirror) {
		return new Mirrors(construction, mirror, naming, parameterisation,
				scoping);
	}

	public Mirrors parameteriseBy(ParameterisationMirror mirror) {
		return new Mirrors(construction, production, naming, mirror, scoping);
	}
}
