/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.util.regex.Pattern;

/**
 * A set of {@link Package}s described by a pattern.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Packages
		implements PreciserThan<Packages> {

	private static final Pattern PATTERN = Pattern.compile( "^(([a-zA-Z_]{1}[a-zA-Z0-9_]*(\\.[a-zA-Z_]{1}[a-zA-Z0-9_]*)*)(\\.\\*|\\*)?|\\*)$" );

	public static final Packages ALL = new Packages( "*" );

	public static Packages packageAndSubPackagesOf( Class<?> type ) {
		return new Packages( packageNameOf( type ) + "*" );
	}

	public static Packages packageOf( Class<?> type ) {
		return new Packages( packageNameOf( type ) );
	}

	public static Packages packages( String pattern ) {
		if ( !PATTERN.matcher( pattern ).matches() ) {
			throw new IllegalArgumentException( "Not a valid package pattern: " + pattern );
		}
		return new Packages( pattern );
	}

	public static Packages subPackagesOf( Class<?> type ) {
		return new Packages( packageNameOf( type ) + ".*" );
	}

	private static String packageNameOf( Class<?> packageOf ) {
		return packageOf.getPackage().getName();
	}

	private static String packageNameOf( Type<?> packageOf ) {
		return packageOf.isLowerBound()
			? "-NONE-"
			: packageNameOf( packageOf.getRawType() );
	}

	private final String pattern;

	private Packages( String pattern ) {
		super();
		this.pattern = pattern;
	}

	public boolean contains( Type<?> type ) {
		return includesMultiple()
			? pattern.length() == 1
				? true
				: pattern.regionMatches( 0, packageNameOf( type ), 0, pattern.length() - 1 )
			: pattern.equals( packageNameOf( type ) );
	}

	public boolean includesAll() {
		return pattern.equals( "*" );
	}

	public boolean includesMultiple() {
		return pattern.endsWith( "*" );
	}

	public boolean includesOneSpecific() {
		return !includesMultiple();
	}

	public boolean includesSome() {
		return !includesAll();
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Packages && equalTo( ( (Packages) obj ) );
	}

	@Override
	public int hashCode() {
		return pattern.hashCode();
	}

	@Override
	public boolean morePreciseThan( Packages other ) {
		if ( equalTo( other ) ) {
			return false;
		}
		final boolean thisIncludesAll = includesAll();
		final boolean otherIncludesAll = other.includesAll();
		if ( thisIncludesAll || otherIncludesAll ) {
			return !thisIncludesAll;
		}
		if ( includesOneSpecific() ) {
			return other.pattern.equals( pattern + "*" );
		}
		return !other.includesOneSpecific() && other.isSubPackage( this );
	}

	@Override
	public String toString() {
		return pattern;
	}

	public boolean equalTo( Packages other ) {
		return other.pattern.equals( pattern );
	}

	private boolean isSubPackage( Packages other ) {
		final int length = includesMultiple()
			? pattern.length() - 1
			: pattern.length();
		return other.pattern.regionMatches( 0, pattern, 0, length );
	}
}
