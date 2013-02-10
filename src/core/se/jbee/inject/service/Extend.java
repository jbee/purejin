/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.service;

import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;
import se.jbee.inject.Dependency;
import se.jbee.inject.Name;

public final class Extend {

	public static <E extends Enum<E> & Extension<E, ? super T>, T> Name extensionName(
			Class<E> extension, Class<? extends T> type ) {
		return named( extension.getCanonicalName() + ":" + type.getCanonicalName() );
	}

	public static <E extends Enum<E> & Extension<E, ? super T>, T> Name extensionName( E extension,
			Class<? extends T> type ) {
		return named( extension.getClass().getCanonicalName() + ":" + extension.name() + ":"
				+ type.getCanonicalName() );
	}

	public static <E extends Enum<E> & Extension<E, ? super T>, T> Dependency<Class[]> extensionDependency(
			Class<E> extension ) {
		return Dependency.dependency( raw( Class[].class ).parametizedAsLowerBounds() ).named(
				Name.prefixed( extension.getCanonicalName() + ":" ) );
	}
}
