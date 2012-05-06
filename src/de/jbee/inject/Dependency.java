package de.jbee.inject;

/**
 * Describes what is wanted/needed as parameter to construct a instance of T.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 */
public final class Dependency<T>
		implements Typed<T> {

	public static <T> Dependency<T> dependency( Type<T> type ) {
		return new Dependency<T>( type );
	}

	private final Type<T> type;

	private Dependency( Type<T> type ) {
		super();
		this.type = type;
	}

	@Override
	public Type<T> getType() {
		return type;
	}

	public Name getName() {
		// TODO Auto-generated method stub
		return null;
	}

	// also add target hierarchy: the class of the instance that is injected
}
