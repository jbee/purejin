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

	// also add target hierarchy: the class of the instance that is injected
}
