/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.Descriptor;
import se.jbee.inject.Scope;
import se.jbee.inject.bind.ValueBinder;

/**
 * A {@link Constant} is the {@link ValueBinder} expansion wrapper type for any
 * constant bound to in the fluent binder API.
 *
 * @param <T> Type of the constant value
 */
public final class Constant<T> implements Descriptor {

	public final T value;
	public final boolean autoBindExactType;
	/**
	 * True in case the {@link Scope} and its effects should be applied, else
	 * false. By default constants are assumed to be value types that are not
	 * scoped.
	 */
	public final boolean scoped;

	public Constant(T value) {
		this(value, true, false);
	}

	private Constant(T value, boolean autoBindExactType, boolean scoped) {
		this.value = value;
		this.autoBindExactType = autoBindExactType;
		this.scoped = scoped;
	}

	public Constant<T> scoped() {
		return new Constant<>(value, autoBindExactType, true);
	}

	public Constant<T> manual() {
		return new Constant<>(value, false, scoped);
	}

}
