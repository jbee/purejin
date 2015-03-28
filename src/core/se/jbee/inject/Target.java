/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Packages.packageAndSubPackagesOf;
import static se.jbee.inject.Packages.packageOf;
import static se.jbee.inject.Packages.subPackagesOf;
import static se.jbee.inject.Type.raw;

/**
 * Describes where a {@link Resource} is available for injection.
 * 
 * This can be restricted by the {@link Packages} the injected that is injected is defined in or the
 * {@link Type} of the receiving instance.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Target
		implements MorePreciseThan<Target> {

	public static final Target ANY = targeting( Instance.ANY );

	public static Target targeting( Class<?> type ) {
		return targeting( raw( type ) );
	}

	public static Target targeting( Type<?> type ) {
		return targeting( Instance.anyOf( type ) );
	}

	public static Target targeting( Instance<?> instance ) {
		return new Target( Instances.ANY, instance, Packages.ALL );
	}

	private final Instances parents;
	private final Instance<?> instance;
	private final Packages packages;

	private Target( Instances parents, Instance<?> instance, Packages packages ) {
		super();
		this.parents = parents;
		this.instance = instance;
		this.packages = packages;
	}

	public Target within( Instance<?> parent ) {
		return new Target( parents.push( parent ), instance, packages );
	}

	public Target injectingInto( Instance<?> instance ) {
		return new Target( parents, instance, packages );
	}

	public Target in( Packages packages ) {
		return new Target( parents, instance, packages );
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
		if ( !areParentsAdequateFor( dependency ) ) {
			return false;
		}
		if ( instance.isAny() ) {
			return true;
		}
		final Instance<?> target = dependency.target();
		return instance.name.isCompatibleWith( target.name )
				&& injectable( instance.getType(), target.getType() );
	}

	private boolean areParentsAdequateFor( Dependency<?> dependency ) {
		if ( parents.isAny() ) {
			return true;
		}
		int pl = parents.depth();
		int il = dependency.injectionDepth() - 1;
		if ( pl > il ) {
			return false;
		}
		int pi = 0;
		while ( pl <= il && pl > 0 ) {
			if ( injectable( parents.at( pi ).getType(), dependency.target( il ).getType() ) ) {
				pl--;
				pi++;
			}
			il--;
		}
		return pl == 0;
	}

	private static boolean injectable( Type<?> type, Type<?> targetType ) {
		return type.isInterface() || type.isAbstract()
			? targetType.isAssignableTo( type )
			: targetType.equalTo( type );
	}

	public boolean isAccessibleFor( Dependency<?> dependency ) {
		return packages.contains( dependency.target().getType() );
	}

	@Override
	public String toString() {
		String res = "[";
		res += parents.toString() + " ";
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
		return in( packageAndSubPackagesOf( type ) );
	}

	public Target inPackageOf( Class<?> type ) {
		return in( packageOf( type ) );
	}

	public Target inSubPackagesOf( Class<?> type ) {
		return in( subPackagesOf( type ) );
	}

	@Override
	public boolean morePreciseThan( Target other ) {
		final int ol = other.parents.depth();
		final int l = parents.depth();
		if ( ol != l ) {
			return l > ol;
		}
		if ( l > 0 ) { // length is known to be equal
			if ( parents.morePreciseThan( other.parents ) ) {
				return true;
			}
			if ( other.parents.morePreciseThan( parents ) ) {
				return false;
			}
		}
		return Instance.morePreciseThan2( instance, other.instance, packages, other.packages );
	}

	public boolean equalTo( Target other ) {
		return this == other || packages.equalTo( other.packages )
				&& instance.equalTo( other.instance ) && parents.equalTo( other.parents );
	}
}
