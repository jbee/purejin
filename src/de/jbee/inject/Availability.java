package de.jbee.inject;

import static de.jbee.inject.Precision.morePreciseThan2;

public class Availability
		implements PreciserThan<Availability> {

	public static final Availability EVERYWHERE = availability( Instance.ANY );

	public static Availability availability( Instance<?> target ) {
		return new Availability( target, Packages.ALL );
	}

	private final Instance<?> target;
	private final Packages packages;

	private Availability( Instance<?> target, Packages packages ) {
		super();
		this.target = target;
		this.packages = packages;
	}

	public Availability injectingInto( Instance<?> target ) {
		return new Availability( target, packages );
	}

	public boolean isApplicableFor( Dependency<?> dependency ) {
		return isAccessibleFor( dependency ); //TODO target
	}

	public boolean isAccessibleFor( Dependency<?> dependency ) {
		return packages.isMember( dependency.target() );
	}

	@Override
	public String toString() {
		if ( target.isAny() && packages.isAll() ) {
			return "everywhere";
		}
		return "[" + packages + " " + target + "]";
	}

	public Availability inPackageAndSubPackagesOf( Class<?> type ) {
		return within( Packages.withinAndUnder( type ) );
	}

	public Availability inPackageOf( Class<?> type ) {
		return within( Packages.packageOf( type ) );
	}

	public Availability inSubPackagesOf( Class<?> type ) {
		return within( Packages.under( type ) );
	}

	public boolean isTrageted() {
		return !target.isAny();
	}

	@Override
	public boolean morePreciseThan( Availability other ) {
		return morePreciseThan2( packages, other.packages, target, other.target );
	}

	public Availability within( Packages packages ) {
		return new Availability( target, packages );
	}
}
