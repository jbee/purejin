/*
 *  Copyright (c) 2012-2019, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.container;

import static se.jbee.inject.Type.raw;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import se.jbee.inject.Generator;
import se.jbee.inject.InjectionCase;
import se.jbee.inject.Provider;
import se.jbee.inject.Type;

/**
 * Util to get rid of warnings for known generic {@link Type}s.
 * 
 * <b>Implementation Note:</b> storing the the raw type in a var before
 * returning the generic type is a workaround to make this compile with javac
 * (cast works with javaw).
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class Typecast {

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

	public static <T> Type<Factory<T>> factoryTypeOf(Class<T> providedType) {
		return factoryTypeOf(raw(providedType));
	}

	public static <T> Type<Factory<T>> factoryTypeOf(Type<T> providedType) {
		return (Type) raw(Factory.class).parametized(providedType);
	}

	public static <T> Type<Generator<T>> generatorTypeOf(Type<T> providedType) {
		return (Type) raw(Generator.class).parametized(providedType);
	}

	public static <T> Type<Generator<T>[]> generatorsTypeFor(
			Type<T> generatedType) {
		return (Type) raw(Generator[].class).parametized(generatedType);
	}

	public static <T> Type<InjectionCase<T>> injectionCaseTypeFor(
			Class<T> generatedType) {
		return injectionCaseTypeFor(raw(generatedType));
	}

	public static <T> Type<InjectionCase<T>> injectionCaseTypeFor(
			Type<T> generatorType) {
		return (Type) raw(InjectionCase.class).parametized(generatorType);
	}

	public static <T> Type<InjectionCase<T>[]> injectionCasesTypeFor(
			Class<T> generatedType) {
		return injectionCasesTypeFor(raw(generatedType));
	}

	public static <T> Type<InjectionCase<T>[]> injectionCasesTypeFor(
			Type<T> generatedType) {
		return (Type) raw(InjectionCase[].class).parametized(generatedType);
	}

	public static <T> Type<Initialiser<T>> initialiserTypeOf(
			Class<T> intialisedType) {
		return (Type) raw(Initialiser.class).parametized(intialisedType);
	}

	public static <T> Type<Initialiser<T>> initialiserTypeOf(
			Type<T> intialisedType) {
		return (Type) raw(Initialiser.class).parametized(intialisedType);
	}

}
