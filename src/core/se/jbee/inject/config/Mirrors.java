package se.jbee.inject.config;

public final class Mirrors {

	//TODO can Mirrors be Options?

	public static final Mirrors DEFAULT = new Mirrors(ConstructsBy.common,
			ProducesBy.noMethods, NamesBy.defaultName,
			HintsBy.noParameters, ScopesBy.alwaysDefault);

	public final ConstructsBy construction;
	public final ProducesBy production;
	public final NamesBy naming;
	public final HintsBy parameterisation;
	public final ScopesBy scoping;

	private Mirrors(ConstructsBy construction,
			ProducesBy production, NamesBy naming,
			HintsBy parameterisation, ScopesBy scoping) {
		this.construction = construction;
		this.production = production;
		this.naming = naming;
		this.parameterisation = parameterisation;
		this.scoping = scoping;
	}

	public Mirrors scopeBy(ScopesBy mirror) {
		return new Mirrors(construction, production, naming, parameterisation,
				mirror);
	}

	public Mirrors constructBy(ConstructsBy mirror) {
		return new Mirrors(mirror, production, naming, parameterisation,
				scoping);
	}

	public Mirrors nameBy(NamesBy mirror) {
		return new Mirrors(construction, production, mirror, parameterisation,
				scoping);
	}

	public Mirrors produceBy(ProducesBy mirror) {
		return new Mirrors(construction, mirror, naming, parameterisation,
				scoping);
	}

	public Mirrors parameteriseBy(HintsBy mirror) {
		return new Mirrors(construction, production, naming, mirror, scoping);
	}
}
