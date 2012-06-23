package de.jbee.inject;

public class Packages
		implements PreciserThan<Packages> {

	public static final Packages ALL = new Packages( "*" );

	public static Packages withinAndUnder( Class<?> packageOf ) {
		return new Packages( path( packageOf ) + "*" );
	}

	public static Packages packageOf( Class<?> type ) {
		return new Packages( path( type ) );
	}

	public static Packages under( Class<?> packageOf ) {
		return new Packages( path( packageOf ) + ".*" );
	}

	public static Packages packages( String pattern ) {
		return new Packages( pattern );
	}

	private final String pattern;

	private Packages( String pattern ) {
		super();
		this.pattern = pattern;
	}

	@Override
	public boolean morePreciseThan( Packages other ) {
		if ( equalTo( other ) ) {
			return false;
		}
		boolean localSelf = isLocal();
		if ( localSelf && other.isAll() ) {
			return true;
		}
		boolean localOther = other.isLocal();
		if ( localOther && isAll() ) {
			return false;
		}
		if ( isSpecific() ) {
			return other.pattern.equals( pattern + "*" );
		}
		if ( other.isSpecific() ) {
			return pattern.equals( other.pattern + "*" );
		}
		return localSelf && localOther && other.subpath( this );
	}

	private boolean subpath( Packages other ) {
		return other.pattern.regionMatches( 0, pattern, 0, isGroup()
			? pattern.length() - 1
			: pattern.length() );
	}

	public boolean isMember( Type<?> type ) {
		return isGroup()
			? pattern.length() == 1
				? true
				: pattern.regionMatches( 0, path( type ), 0, pattern.length() - 1 )
			: pattern.equals( path( type ) );
	}

	private boolean isGroup() {
		return pattern.endsWith( "*" );
	}

	private static String path( Type<?> packageOf ) {
		return path( packageOf.getRawType() );
	}

	private static String path( Class<?> packageOf ) {
		return packageOf.getPackage().getName();
	}

	public boolean isSpecific() {
		return !isGroup();
	}

	public boolean isLocal() {
		return !isAll();
	}

	public boolean isAll() {
		return pattern.equals( "*" );
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Packages && equalTo( ( (Packages) obj ) );
	}

	private boolean equalTo( Packages other ) {
		return other.pattern.equals( pattern );
	}

	@Override
	public int hashCode() {
		return pattern.hashCode();
	}

	@Override
	public String toString() {
		return pattern;
	}
}
