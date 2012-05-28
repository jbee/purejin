package de.jbee.inject;

/**
 * Gives read-only access to the binds done.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public final class Binding<T> {

	private final Resource<T> resource;
	private final Supplier<? extends T> supplier;
	private final Repository repository;
	private final Source source;

	Binding( Resource<T> resource, Supplier<? extends T> supplier, Repository repository,
			Source source ) {
		super();
		this.resource = resource;
		this.supplier = supplier;
		this.repository = repository;
		this.source = source;
	}

	public Resource<T> getResource() {
		return resource;
	}

	public Supplier<? extends T> getSupplier() {
		return supplier;
	}

	public Source getSource() {
		return source;
	}

	public Repository getRepository() {
		return repository;
	}
}
