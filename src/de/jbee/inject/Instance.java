package de.jbee.inject;

import static de.jbee.inject.Precision.morePreciseThan2;

/**
 * Used to tell that we don#t want just one singleton at a time but multiple distinguished by the
 * {@link Name} used.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public final class Instance<T>
		implements Typed<T>, Named, PreciserThan<Instance<?>> {

	/**
	 * When a wildcard-type is used as bound instance type the bind will be added to all concrete
	 * binds of matching types. There is also a set of wildcard binds that are tried if no bind has
	 * been made for a type.
	 */
	public static final Instance<? extends Object> ANY = anyOf( Type.WILDCARD );

	public static <T> Instance<T> defaultInstanceOf( Type<T> type ) {
		return instance( Name.DEFAULT, type );
	}

	public static <T> Instance<T> anyOf( Type<T> type ) {
		return instance( Name.ANY, type );
	}

	public static <T> Instance<T> instance( Name name, Type<T> type ) {
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
		return type.equalTo( other.type ) && name.equals( other.name );
	}

	public Instance<T> discriminableBy( Name name ) {
		return new Instance<T>( name, type );
	}

	@Override
	public Type<T> getType() {
		return type;
	}

	@Override
	public <E> Instance<E> typed( Type<E> type ) {
		return new Instance<E>( name, type );
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

	public boolean isAny() {
		return name.isAny() && type.equalTo( ANY.type );
	}

	@Override
	public boolean morePreciseThan( Instance<?> other ) {
		// sequence in OR is very important!!!
		return morePreciseThan2( type, other.type, name, other.name );
	}

	public boolean includes( Instance<?> other ) {
		//TODO other cases
		return type.includes( other.type ) && name.equalTo( other.name );
	}
}
