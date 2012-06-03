package de.jbee.inject;

/**
 * Describes what is wanted/needed as parameter to construct a instance of T.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 */
public final class Dependency<T>
		implements Typed<T>, Named {

	public static <T> Dependency<T> dependency( Type<T> type ) {
		return new Dependency<T>( Name.ANY, type );
	}

	private final Name name;
	private final Type<T> type;

	private Dependency( Name name, Type<T> type ) {
		super();
		this.name = name;
		this.type = type;
	}

	@Override
	public Type<T> getType() {
		return type;
	}

	@Override
	public Name getName() {
		return name;
	}

	@Override
	public String toString() {
		return ( name + " " + type ).trim();
	}

	public Dependency<?> onTypeParameter() {
		return dependency( type.getParameters()[0] );
	}

	public <E> Dependency<E> typed( Type<E> type ) {
		return new Dependency<E>( name, type );
	}

	private static <T> Dependency<T> defaultDependency( Type<T> type ) {
		return new Dependency<T>( Name.DEFAULT, type );
	}

	// also add target hierarchy: the class of the instance that is injected
}
