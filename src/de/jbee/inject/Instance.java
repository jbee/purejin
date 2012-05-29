package de.jbee.inject;

/**
 * Used to tell that we don#t want just one singleton at a time but multiple distinguished by the
 * {@link Name} used.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public final class Instance<T>
		implements Typed<T>, Named {

	/**
	 * When a wildcard-type is used as bound instance type the bind will be added to all concrete
	 * binds of matching types. There is also a set of wildcard binds that are tried if no bind has
	 * been made for a type.
	 */
	public static final Instance<? extends Object> ANY = anyOf( Type.rawType( Object.class ).asLowerBound() );

	public static <T> Instance<T> defaultInstanceOf( Type<T> type ) {
		return named( Name.DEFAULT, type );
	}

	public static <T> Instance<T> anyOf( Type<T> type ) {
		return named( Name.ANY, type );
	}

	public static <T> Instance<T> named( Name name, Type<T> type ) {
		return new Instance<T>( name, type );
	}

	private final Name name;
	private final Type<T> type;

	private Instance( Name name, Type<T> type ) {
		super();
		this.name = name;
		this.type = type;
	}

	public boolean equalTo( Instance<T> other ) {

		return true;
	}

	public Instance<T> discriminableBy( Name name ) {
		return new Instance<T>( name, type );
	}

	@Override
	public Type<T> getType() {
		return type;
	}

	@Override
	public Name getName() {
		return name;
	}

	public Resource<T> toResource() {
		return new Resource<T>( this );
	}

	@Override
	public String toString() {
		return name + " " + type;
	}
}
