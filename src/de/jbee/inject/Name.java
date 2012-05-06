package de.jbee.inject;

/**
 * A {@link Name} is used as discriminator in cases where multiple {@link Instance}s are bound for
 * the same {@link Type}.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Name {

	public static final Name DEFAULT = new Name( "" );

	private final String value;

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

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Name && ( (Name) obj ).value.equals( value );
	}

	public boolean isApplicableFor( Name name ) {
		// TODO Auto-generated method stub
		return true;
	}
}
