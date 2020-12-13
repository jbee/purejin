/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.bind;

import se.jbee.inject.Supplier;

/**
 * What is supplying instances for a {@link Binding}?
 *
 * As we cannot look into the implementation of a {@link Supplier} this type
 * groups ways to supply instances.
 */
public enum BindingType {

	/**
	 * The binding expresses a need, it is unclear if another binding can fulfil
	 * it.
	 */
	REQUIRED,

	/**
	 * The binding is a forward reference to a sub-type (implementation type) or
	 * to a virtual or generic instance factory like one for lists or other type
	 * parameterised "bridges".
	 */
	REFERENCE,

	/**
	 * The instances are supplied from a {@link Supplier} that has been defined
	 * before expansion. This might be user defined or hard-wired one within the
	 * binder API.
	 */
	PREDEFINED,

	/**
	 * The instances are supplied by constructing new ones using a constructor.
	 */
	CONSTRUCTOR,

	/**
	 * The instances are supplied using a factory method.
	 */
	METHOD,

	/**
	 * The instance is supplied from a field. Field is read each time supply
	 * occurs.
	 */
	FIELD,

	/**
	 * The binding is an incomplete value that should be expanded into a
	 * complete {@link Binding}.
	 */
	VALUE,
}
