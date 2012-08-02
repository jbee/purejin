package de.jbee.inject;

import static de.jbee.inject.Precision.morePreciseThan2;
import static de.jbee.inject.Type.raw;

public final class Target
		implements PreciserThan<Target> {

	public static final Target ANY = targeting( Instance.ANY );

	public static Target targeting( Class<?> type ) {
		return targeting( raw( type ) );
	}

	public static Target targeting( Type<?> type ) {
		return targeting( Instance.anyOf( type ) );
	}

	public static Target targeting( Instance<?> instance ) {
		return new Target( instance, Packages.ALL );
	}

	private final Instance<?> instance;
	private final Packages packages;

	private Target( Instance<?> instance, Packages packages ) {
		super();
		this.instance = instance;
		this.packages = packages;
	}

	public Target injectingInto( Instance<?> instance ) {
		return new Target( instance, packages );
	}

	public Target injectingInto( Type<?> type ) {
		return injectingInto( Instance.anyOf( type ) );
	}

	public Target injectingInto( Class<?> type ) {
		return injectingInto( raw( type ) );
	}

	public boolean isApplicableFor( Dependency<?> dependency ) {
		return isAccessibleFor( dependency ) && isAdequateFor( dependency );
	}

	public boolean isAdequateFor( Dependency<?> dependency ) {
		if ( instance.isAny() ) {
			return true;
		}
		final Instance<?> target = dependency.target();
		if ( !instance.getName().isApplicableFor( target.getName() ) ) {
			return false;
		}
		final Type<?> type = instance.getType();
		return type.isInterface()
			? target.getType().isAssignableTo( type )
			: target.getType().equalTo( type );
	}

	public boolean isAccessibleFor( Dependency<?> dependency ) {
		return packages.contains( dependency.target().getType() );
	}

	@Override
	public String toString() {
		String res = "[";
		res += instance.isAny()
			? "anything"
			: instance.toString();
		res += " ";
		res += packages.includesAll()
			? "anywhere"
			: packages.toString();
		return res + "]";
	}

	public Target inPackageAndSubPackagesOf( Class<?> type ) {
		return within( Packages.packageAndSubPackagesOf( type ) );
	}

	public Target inPackageOf( Class<?> type ) {
		return within( Packages.packageOf( type ) );
	}

	public Target inSubPackagesOf( Class<?> type ) {
		return within( Packages.subPackagesOf( type ) );
	}

	public boolean isSpecificInstance() {
		return !instance.isAny();
	}

	@Override
	public boolean morePreciseThan( Target other ) {
		return morePreciseThan2( packages, other.packages, instance, other.instance );
	}

	public Target within( Packages packages ) {
		return new Target( instance, packages );
	}

	public boolean equalTo( Target other ) {
		return packages.equalTo( other.packages ) && instance.equalTo( other.instance );
	}
}
