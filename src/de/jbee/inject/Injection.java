package de.jbee.inject;

public class Injection<T> {

	private final Dependency<? super T> dependency;
	private final int serialNumber;
	private final int cardinality;

	public Injection( Dependency<? super T> dependency, int serialNumber, int cardinality ) {
		super();
		this.dependency = dependency;
		this.serialNumber = serialNumber;
		this.cardinality = cardinality;
	}

	public Dependency<? super T> dependency() {
		return dependency;
	}

	/**
	 * @return the number of the {@link Injectron} being injected.
	 */
	public final int serialNumber() {
		return serialNumber;
	}

	/**
	 * @return the total amount of {@link Injectron} in the same context (injector).
	 */
	public final int cardinality() {
		return cardinality;
	}

	public Injection<T> on( Dependency<? super T> dependency ) {
		return new Injection<T>( dependency, serialNumber, cardinality );
	}

	@Override
	public String toString() {
		return dependency + "[" + serialNumber + "/" + cardinality + "]";
	}
}
