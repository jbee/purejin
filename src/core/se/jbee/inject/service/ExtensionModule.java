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
import se.jbee.inject.bind.Binder;
import se.jbee.inject.bind.BinderModule;

public abstract class ExtensionModule
		extends BinderModule {

	public static <E extends Enum<E> & Extension<E, ? super T>, T> void extend( Binder binder,
			Class<E> extension, Class<? extends T> type ) {
		binder.multibind( extensionName( extension, type ), Class.class ).to( type );
		binder.bind( type ).toConstructor(); //TODO implicit
	}

	public static <E extends Enum<E> & Extension<E, ? super T>, T> void extend( Binder binder,
			E extension, Class<? extends T> type ) {
		binder.multibind( extensionName( extension, type ), Class.class ).to( type );
		binder.bind( type ).toConstructor(); //TODO implicit
	}

	public static <E extends Enum<E> & Extension<E, ? super T>, T> Name extensionName(
			Class<E> extension, Class<? extends T> type ) {
		return named( extension.getCanonicalName() + ":" + type.getCanonicalName() );
	}

	public static <E extends Enum<E> & Extension<E, ? super T>, T> Name extensionName( E extension,
			Class<? extends T> type ) {
		return named( extension.getClass().getCanonicalName() + ":" + extension.name() + ":"
				+ type.getCanonicalName() );
	}

	@SuppressWarnings ( { "rawtypes" } )
	public static <E extends Enum<E> & Extension<E, ? super T>, T> Dependency<Class[]> extensionDependency(
			Class<E> extension ) {
		return Dependency.dependency( raw( Class[].class ).parametizedAsLowerBounds() ).named(
				Name.prefixed( extension.getCanonicalName() + ":" ) );
	}
}
