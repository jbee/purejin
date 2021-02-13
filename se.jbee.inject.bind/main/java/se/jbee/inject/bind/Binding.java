/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.*;
import se.jbee.lang.Qualifying;
import se.jbee.lang.Type;
import se.jbee.lang.Typed;

/**
 * A {@link Binding} is a {@link ResourceDescriptor} created during the
 * bootstrapping process based on {@link Bindings}, {@link Bundle}s and {@link
 * Module}s.
 *
 * @param <T> The type of the bound value (instance)
 */
public final class Binding<T> extends ResourceDescriptor<T>
		implements Module, Typed<T>, Descriptor, Comparable<Binding<?>> {

	public static <T> Binding<T> binding(Locator<T> signature, BindingType type,
			Supplier<? extends T> supplier, Name scope, Source source) {
		return new Binding<>(signature, type, supplier, scope, source,
				annotatedOf(supplier), Verifier.AOK);
	}

	public final BindingType type;

	private Binding(Locator<T> signature, BindingType type,
			Supplier<? extends T> supplier, Name scope, Source source,
			Annotated annotations, Verifier verifier) {
		super(scope, signature, supplier, source, annotations, verifier);
		this.type = type;
	}

	@Override
	public Type<T> type() {
		return signature.type();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> Binding<E> typed(Type<E> type) {
		signature.type().castTo(type); // make sure it is a valid supertype
		return new Binding<>(signature.typed(type),
				this.type, (Supplier<? extends E>) supplier, scope, source,
				annotations, verifier);
	}

	public boolean isComplete() {
		return supplier != null;
	}

	@Override
	public Binding<T> annotatedBy(Annotated annotations) {
		if (annotations == this.annotations)
			return this; // just a optimisation for a likely case
		return new Binding<>(signature, type, supplier, scope, source,
				annotations, verifier);
	}

	@Override
	public Binding<T> verifiedBy(Verifier verifier) {
		if (verifier == this.verifier)
			return this;
		return new Binding<>(signature, type, supplier, scope, source,
				annotations, verifier);
	}

	public Binding<T> complete(BindingType type,
			Supplier<? extends T> supplier) {
		if (type == BindingType.VALUE)
			throw InconsistentBinding.illegalCompletion(this, type);
		Name effectiveScope = type == BindingType.REFERENCE
			? Scope.reference
			: scope;
		return new Binding<>(signature, type, supplier, effectiveScope, source,
				annotatedOf(supplier), verifier);
	}

	@Override
	public void declare(Bindings bindings, Env env) {
		bindings.add(env, this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Binding))
			return false;
		Binding<?> other = (Binding<?>) obj;
		return signature.equalTo(other.signature)
			&& source.equalTo(other.source) && scope.equalTo(other.scope)
			&& type == other.type;
	}

	@Override
	public int hashCode() {
		return signature.hashCode() ^ source.hashCode();
	}

	@Override
	public int compareTo(Binding<?> other) {
		int res = signature.type().rawType.getName().compareTo(
				other.signature.type().rawType.getName());
		if (res != 0)
			return res;
		res = Qualifying.compare(signature.instance, other.signature.instance);
		if (res != 0)
			return res;
		res = signature.instance.compareTo(other.signature.instance);
		if (res != 0)
			return res;
		res = Qualifying.compare(signature.target, other.signature.target);
		if (res != 0)
			return res;
		res = signature.target.compareTo(other.signature.target);
		if (res != 0)
			return res;
		res = Qualifying.compare(source, other.source);
		if (res != 0)
			return res;
		res = source.compareTo(other.source);
		if (res != 0)
			return res;
		res = scope.compareTo(other.scope);
		if (res != 0)
			return res;
		return type.compareTo(other.type);
	}
}
