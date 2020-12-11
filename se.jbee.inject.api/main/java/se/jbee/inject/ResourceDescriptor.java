/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.util.List;

/**
 * A "construction description" for a particular instance or set of kind of
 * instances.
 *
 * A {@link List} would be a generic {@link ResourceDescriptor}, some singleton
 * "service" a non-generic one.
 *
 * Each {@link ResourceDescriptor} becomes an {@link Resource} and {@link Generator}
 * within the {@link Injector} (1:1 relation).
 *
 * The class is mainly introduced to decouple the everything on top of the
 * container module (which is the core) from the container implementation
 * itself. In the concrete case the bootstrap package should only depend on the
 * container but not vice versa. This also allows to build custom bootstrapping
 * (utility) packages as only contract is a consistent list of
 * {@link ResourceDescriptor} is produced to build a {@link Injector}s context.
 *
 * @since 8.1
 *
 * @param <T> Type of instances injected.
 */
public abstract class ResourceDescriptor<T> {

	public final Name scope;
	public final Locator<T> signature;
	public final Source source;
	public final Supplier<? extends T> supplier;
	public final Annotated annotations;
	public final Verifier verifier;

	public ResourceDescriptor(Name scope, Locator<T> signature,
			Supplier<? extends T> supplier, Source source,
			Annotated annotations, Verifier verifier) {
		this.scope = scope;
		this.signature = signature;
		this.supplier = supplier;
		this.source = source;
		this.annotations = annotations;
		this.verifier = verifier;
	}

	public static Annotated annotatedOf(Supplier<?> supplier) {
		return supplier instanceof Annotated
			? (Annotated) supplier
			: Annotated.EMPTY;
	}

	public abstract ResourceDescriptor<T> annotatedBy(Annotated annotations);

	public abstract ResourceDescriptor<T> verifiedBy(Verifier verifier);

	@Override
	public String toString() {
		return signature + " / " + scope + " / " + source;
	}
}
