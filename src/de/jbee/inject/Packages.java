package de.jbee.inject;

import java.util.regex.Pattern;

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
		return packageNameOf( packageOf.getRawType() );
	}

	private final String pattern;

	private Packages( String pattern ) {
		super();
		this.pattern = pattern;
	}

	public boolean contains( Type<?> type ) {
		return containsMultiple()
			? pattern.length() == 1
				? true
				: pattern.regionMatches( 0, packageNameOf( type ), 0, pattern.length() - 1 )
			: pattern.equals( packageNameOf( type ) );
	}

	public boolean containsAll() {
		return pattern.equals( "*" );
	}

	public boolean containsMultiple() {
		return pattern.endsWith( "*" );
	}

	public boolean containsOneSpecific() {
		return !containsMultiple();
	}

	public boolean containsSome() {
		return !containsAll();
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
		boolean someInThin = containsSome();
		if ( someInThin && other.containsAll() ) {
			return true;
		}
		boolean someInOther = other.containsSome();
		if ( someInOther && containsAll() ) {
			return false;
		}
		if ( containsOneSpecific() ) {
			return other.pattern.equals( pattern + "*" );
		}
		if ( other.containsOneSpecific() ) {
			return pattern.equals( other.pattern + "*" );
		}
		return someInThin && someInOther && other.isSubPackage( this );
	}

	@Override
	public String toString() {
		return pattern;
	}

	private boolean equalTo( Packages other ) {
		return other.pattern.equals( pattern );
	}

	private boolean isSubPackage( Packages other ) {
		return other.pattern.regionMatches( 0, pattern, 0, containsMultiple()
			? pattern.length() - 1
			: pattern.length() );
	}
}
