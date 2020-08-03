/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Type.raw;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Utility to get rid of warnings for known generic {@link Type}s.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class Cast {

	private Cast() {
		throw new UnsupportedOperationException("util");
	}

	public static <T> Type<List<T>> listTypeOf(Class<T> elementType) {
		return listTypeOf(raw(elementType));
	}

	public static <T> Type<List<T>> listTypeOf(Type<T> elementType) {
		return (Type) raw(List.class).parametized(elementType);
	}

	public static <T> Type<Set<T>> setTypeOf(Class<T> elementType) {
		return setTypeOf(raw(elementType));
	}

	public static <T> Type<Set<T>> setTypeOf(Type<T> elementType) {
		return (Type) raw(Set.class).parametized(elementType);
	}

	public static <T> Type<Collection<T>> collectionTypeOf(
			Class<T> elementType) {
		return collectionTypeOf(raw(elementType));
	}

	public static <T> Type<Collection<T>> collectionTypeOf(
			Type<T> elementType) {
		return (Type) raw(Collection.class).parametized(elementType);
	}

	public static <T> Type<Provider<T>> providerTypeOf(Class<T> providedType) {
		return providerTypeOf(raw(providedType));
	}

	public static <T> Type<Provider<T>> providerTypeOf(Type<T> providedType) {
		return (Type) raw(Provider.class).parametized(providedType);
	}

	public static <T> Type<Generator<T>> generatorTypeOf(Type<T> providedType) {
		return (Type) raw(Generator.class).parametized(providedType);
	}

	public static <T> Type<Generator<T>[]> generatorsTypeFor(
			Type<T> generatedType) {
		return (Type) raw(Generator[].class).parametized(generatedType);
	}

	public static <T> Type<Resource<T>> resourceTypeFor(Class<T> type) {
		return resourceTypeFor(raw(type));
	}

	public static <T> Type<Resource<T>> resourceTypeFor(Type<T> type) {
		return (Type) raw(Resource.class).parametized(type);
	}

	public static <T> Type<Resource<T>[]> resourcesTypeFor(Class<T> type) {
		return resourcesTypeFor(raw(type));
	}

	public static <T> Type<Resource<T>[]> resourcesTypeFor(Type<T> type) {
		return (Type) raw(Resource[].class).parametized(type);
	}

	public static <T> Type<Initialiser<T>> initialiserTypeOf(
			Class<T> initialisedType) {
		return (Type) raw(Initialiser.class).parametized(initialisedType);
	}

	public static <T> Type<Initialiser<T>> initialiserTypeOf(
			Type<T> initialisedType) {
		return (Type) raw(Initialiser.class).parametized(initialisedType);
	}

}
