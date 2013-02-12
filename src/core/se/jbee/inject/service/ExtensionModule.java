/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.service;

import se.jbee.inject.bind.Binder;
import se.jbee.inject.bind.BinderModule;

public abstract class ExtensionModule
		extends BinderModule {

	public static <E extends Enum<E> & Extension<E, ? super T>, T> void extend( Binder binder,
			Class<E> extension, Class<? extends T> type ) {
		binder.multibind( Extend.extensionName( extension, type ), Class.class ).to( type );
		binder.bind( type ).toConstructor(); //TODO implicit
	}

	public static <E extends Enum<E> & Extension<E, ? super T>, T> void extend( Binder binder,
			E extension, Class<? extends T> type ) {
		binder.multibind( Extend.extensionName( extension, type ), Class.class ).to( type );
		binder.bind( type ).toConstructor(); //TODO implicit
	}
}
