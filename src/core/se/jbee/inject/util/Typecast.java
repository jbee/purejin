/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.util;

import static se.jbee.inject.Type.raw;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import se.jbee.inject.Injectron;
import se.jbee.inject.Type;

/**
 * Util to get rid of warnings for known generic {@link Type}s.
 * 
 * <b>Implementation Note:</b> storing the the raw type in a var before returning the generic type
 * is a workaround to make this compile with javac (cast works with javaw).
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Typecast {

	public static <T> Type<List<T>> listTypeOf( Class<T> elementType ) {
		return listTypeOf( raw( elementType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<List<T>> listTypeOf( Type<T> elementType ) {
		Type raw = raw( List.class ).parametized( elementType );
		return raw;
	}

	public static <T> Type<Set<T>> setTypeOf( Class<T> elementType ) {
		return setTypeOf( raw( elementType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<Set<T>> setTypeOf( Type<T> elementType ) {
		Type raw = raw( Set.class ).parametized( elementType );
		return raw;
	}

	public static <T> Type<Collection<T>> collectionTypeOf( Class<T> elementType ) {
		return collectionTypeOf( raw( elementType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<Collection<T>> collectionTypeOf( Type<T> elementType ) {
		Type raw = raw( Collection.class ).parametized( elementType );
		return raw;
	}

	public static <T> Type<Provider<T>> providerTypeOf( Class<T> providedType ) {
		return providerTypeOf( raw( providedType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<Provider<T>> providerTypeOf( Type<T> providedType ) {
		Type raw = raw( Provider.class ).parametized( providedType );
		return raw;
	}

	public static <T> Type<Factory<T>> factoryTypeOf( Class<T> providedType ) {
		return factoryTypeOf( raw( providedType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<Factory<T>> factoryTypeOf( Type<T> providedType ) {
		Type raw = raw( Factory.class ).parametized( providedType );
		return raw;
	}

	public static <T> Type<Injectron<T>> injectronTypeOf( Class<T> providedType ) {
		return injectronTypeOf( raw( providedType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<Injectron<T>> injectronTypeOf( Type<T> providedType ) {
		Type raw = raw( Injectron.class ).parametized( providedType );
		return raw;
	}

	public static <T> Type<Injectron<T>[]> injectronsTypeOf( Class<T> providedType ) {
		return injectronsTypeOf( raw( providedType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<Injectron<T>[]> injectronsTypeOf( Type<T> providedType ) {
		Type raw = raw( Injectron[].class ).parametized( providedType );
		return raw;
	}
}
