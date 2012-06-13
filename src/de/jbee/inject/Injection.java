package de.jbee.inject;

public class Injection<T> {

	private final Dependency<? super T> dependency;
	private final int injectronSerialNumber;
	private final int injectronCardinality;

	public Injection( Dependency<? super T> dependency, int injectronSerialNumber,
			int injectronCardinality ) {
		super();
		this.dependency = dependency;
		this.injectronSerialNumber = injectronSerialNumber;
		this.injectronCardinality = injectronCardinality;
	}

	public Dependency<? super T> getDependency() {
		return dependency;
	}

	public final int injectronSerialNumber() {
		return injectronSerialNumber;
	}

	public final int injectronCardinality() {
		return injectronCardinality;
	}

	public Injection<T> on( Dependency<? super T> dependency ) {
		return new Injection<T>( dependency, injectronSerialNumber, injectronCardinality );
	}

	@Override
	public String toString() {
		return dependency + "[" + injectronCardinality + "/" + injectronCardinality + "]";
	}
}
