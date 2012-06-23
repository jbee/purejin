package de.jbee.inject.bind;

import de.jbee.inject.Repository;
import de.jbee.inject.Resource;
import de.jbee.inject.Source;
import de.jbee.inject.Supplier;

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

	public Binding( Resource<T> resource, Supplier<? extends T> supplier, Repository repository,
			Source source ) {
		super();
		this.resource = resource;
		this.supplier = supplier;
		this.repository = repository;
		this.source = source;
	}

	public Resource<T> resource() {
		return resource;
	}

	public Supplier<? extends T> supplier() {
		return supplier;
	}

	public Source source() {
		return source;
	}

	public Repository repository() {
		return repository;
	}

	@Override
	public String toString() {
		return source + " / " + resource + " / " + supplier;
	}
}
