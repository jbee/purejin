package de.jbee.inject.util;

import static de.jbee.inject.Type.raw;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import de.jbee.inject.Injectron;
import de.jbee.inject.Type;

public final class Typecast {

	public static <T> Type<? extends List<T>> listTypeOf( Class<T> elementType ) {
		return listTypeOf( raw( elementType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends List<T>> listTypeOf( Type<T> elementType ) {
		return (Type<? extends List<T>>) raw( List.class ).parametized( elementType );
	}

	public static <T> Type<? extends Set<T>> setTypeOf( Class<T> elementType ) {
		return setTypeOf( raw( elementType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends Set<T>> setTypeOf( Type<T> elementType ) {
		return (Type<? extends Set<T>>) raw( Set.class ).parametized( elementType );
	}

	public static <T> Type<? extends Collection<T>> collectionTypeOf( Class<T> elementType ) {
		return collectionTypeOf( raw( elementType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends Collection<T>> collectionTypeOf( Type<T> elementType ) {
		return (Type<? extends Collection<T>>) raw( Collection.class ).parametized( elementType );
	}

	public static <T> Type<? extends Provider<T>> providerTypeOf( Class<T> providedType ) {
		return providerTypeOf( raw( providedType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends Provider<T>> providerTypeOf( Type<T> providedType ) {
		return (Type<? extends Provider<T>>) raw( Provider.class ).parametized( providedType );
	}

	public static <T> Type<? extends Factory<T>> factoryTypeOf( Class<T> providedType ) {
		return factoryTypeOf( raw( providedType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends Factory<T>> factoryTypeOf( Type<T> providedType ) {
		return (Type<? extends Factory<T>>) raw( Factory.class ).parametized( providedType );
	}

	public static <T> Type<? extends Injectron<T>> injectronTypeOf( Class<T> providedType ) {
		return injectronTypeOf( raw( providedType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends Injectron<T>> injectronTypeOf( Type<T> providedType ) {
		return (Type<? extends Injectron<T>>) raw( Injectron.class ).parametized( providedType );
	}

	public static <T> Type<? extends Injectron<T>[]> injectronsTypeOf( Class<T> providedType ) {
		return injectronsTypeOf( raw( providedType ) );
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<? extends Injectron<T>[]> injectronsTypeOf( Type<T> providedType ) {
		return (Type<? extends Injectron<T>[]>) raw( Injectron[].class ).parametized( providedType );
	}
}
