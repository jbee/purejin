package de.jbee.silk;

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
	private final int injectronSerialNumber;
	private final int injectronCardinality;

	private Dependency( Type<T> type ) {
		this( type, -1, -1 );
	}

	private Dependency( Type<T> type, int injectronSerialNumber, int injectronCardinality ) {
		super();
		this.type = type;
		this.injectronSerialNumber = injectronSerialNumber;
		this.injectronCardinality = injectronCardinality;
	}

	@Override
	public Type<T> getType() {
		return type;
	}

	public int injectronSerialNumber() {
		return injectronSerialNumber;
	}

	public int injectronCardinality() {
		return injectronCardinality;
	}

	public Dependency<T> onInjectronCardinality( int cardinality ) {
		return new Dependency<T>( type, injectronSerialNumber, cardinality );
	}

	public Dependency<T> onInjectronSerialNumber( int serialNumber ) {
		return new Dependency<T>( type, serialNumber, injectronCardinality );
	}
	// also add target hierarchy: the class of the instance that is injected
}
