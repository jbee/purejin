package de.jbee.inject;

public class Resolution<T> {

	private final Resource<T> resource;
	private final int serialNumber;
	private final int cardinality;
	private final Dependency<? super T> dependency;

	public Resolution( Resource<T> resource, Dependency<? super T> dependency, int serialNumber,
			int cardinality ) {
		super();
		this.resource = resource;
		this.dependency = dependency;
		this.serialNumber = serialNumber;
		this.cardinality = cardinality;
	}

	public Dependency<? super T> dependency() {
		return dependency;
	}

	public Resource<T> resource() {
		return resource;
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

	public Resolution<T> on( Dependency<? super T> dependency ) {
		return new Resolution<T>( resource, dependency, serialNumber, cardinality );
	}

	@Override
	public String toString() {
		return serialNumber + " " + dependency;
	}
}
