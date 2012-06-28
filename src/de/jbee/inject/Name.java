package de.jbee.inject;

/**
 * A {@link Name} is used as discriminator in cases where multiple {@link Instance}s are bound for
 * the same {@link Type}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Name
		implements PreciserThan<Name> {

	/**
	 * Used when no name is specified. It is the most precise name of all.
	 */
	public static final Name DEFAULT = new Name( "" );
	/**
	 * It is the least precise name of all.
	 */
	public static final Name ANY = new Name( "*" );

	private final String value;

	public static Name prefixed( String prefix ) {
		return prefix == null || prefix.trim().isEmpty()
			? ANY
			: new Name( prefix + "*" );
	}

	public static Name named( String name ) {
		return name == null || name.trim().isEmpty()
			? DEFAULT
			: new Name( name );
	}

	private Name( String value ) {
		super();
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	public Name or( Name other ) {
		return value.equals( other.value )
			? this
			: new Name( value + "|" + other.value );
	}

	public boolean equalTo( Name other ) {
		return value.equalsIgnoreCase( other.value );
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Name && equalTo( (Name) obj );
	}

	public boolean isAny() {
		return value.equals( ANY.value );
	}

	public boolean isDefault() {
		return value.isEmpty();
	}

	@Override
	public boolean morePreciseThan( Name other ) {
		final boolean thisIsDefault = isDefault();
		final boolean otherIsDefault = other.isDefault();
		if ( thisIsDefault || otherIsDefault ) {
			return !otherIsDefault;
		}
		final boolean thisIsAny = isAny();
		final boolean otherIsAny = other.isAny();
		if ( thisIsAny || otherIsAny ) {
			return !thisIsAny;
		}
		//TODO or multi-names
		return value.startsWith( other.value ) && value.length() > other.value.length();
	}

	public boolean isApplicableFor( Name other ) {
		if ( other.value.indexOf( '|' ) > 0 ) {
			for ( String name : other.value.split( "\\|" ) ) {
				if ( isApplicableFor( named( name ) ) ) {
					return true;
				}
			}
		}
		return isAny() || other.value.equalsIgnoreCase( value )
				|| value.matches( other.value.replace( "*", ".*" ) );
	}

}
