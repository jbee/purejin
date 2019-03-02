/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import static se.jbee.inject.Utils.arrayContains;
import static se.jbee.inject.Utils.arrayEquals;
import static se.jbee.inject.Utils.arrayFindFirst;
import static se.jbee.inject.Utils.arrayMap;
import static se.jbee.inject.Utils.newArray;

import java.io.Serializable;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A generic version of {@link Class} like {@link java.lang.reflect.Type} but
 * without a complex hierarchy. Instead all cases are represented as a general
 * model. The key difference is that this model just describes concrete types.
 * So there is no representation for a {@link TypeVariable}.
 * 
 * There are some generic cases ({@code ? super X} types) that are not supported
 * right now because they haven't been needed.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Type<T>
		implements Qualifying<Type<?>>, Parameter<T>, Serializable {

	public static final Type<Object> OBJECT = Type.raw(Object.class);
	public static final Type<Void> VOID = raw(Void.class);
	public static final Type<?> WILDCARD = OBJECT.asUpperBound();

	public static Type<?> fieldType(Field field) {
		return type(field.getGenericType());
	}

	public static Type<?> returnType(Method method) {
		return type(method.getGenericReturnType());
	}

	public static Type<?>[] parameterTypes(Executable methodOrConstructor) {
		return parameterTypes(methodOrConstructor.getGenericParameterTypes());
	}

	private static Type<?>[] parameterTypes(
			java.lang.reflect.Type[] genericParameterTypes) {
		Type<?>[] res = new Type<?>[genericParameterTypes.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = type(genericParameterTypes[i]);
		}
		return res;
	}

	public static Type<?>[] wildcards(TypeVariable<?>... variables) {
		Type<?>[] wildcards = new Type<?>[variables.length];
		Arrays.fill(wildcards, WILDCARD);
		return wildcards;
	}

	public static <T> Type<T> raw(Class<T> type) {
		return new Type<>(type);
	}

	private static Type<?>[] types(java.lang.reflect.Type[] parameters,
			Map<String, Type<?>> actualTypeArguments) {
		Type<?>[] args = new Type<?>[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			args[i] = type(parameters[i], actualTypeArguments);
		}
		return args;
	}

	private static Type<?> type(java.lang.reflect.Type type) {
		return type(type, Collections.emptyMap());
	}

	private static Type<?> type(java.lang.reflect.Type type,
			Map<String, Type<?>> actualTypeArguments) {
		if (type instanceof Class<?>) {
			return raw((Class<?>) type);
		}
		if (type instanceof ParameterizedType) {
			return parameterizedType((ParameterizedType) type,
					actualTypeArguments);
		}
		if (type instanceof TypeVariable<?>) {
			return actualTypeArguments.get(((TypeVariable<?>) type).getName());
		}
		if (type instanceof GenericArrayType) {
			GenericArrayType gat = (GenericArrayType) type;
			return type(gat.getGenericComponentType()).addArrayDimension();
		}
		if (type instanceof WildcardType) {
			WildcardType wt = (WildcardType) type;
			java.lang.reflect.Type[] upperBounds = wt.getUpperBounds();
			if (upperBounds.length == 1) {
				return type(upperBounds[0]).asUpperBound();
			}
		}
		throw new UnsupportedOperationException(
				"Type has no support yet: " + type);
	}

	private static <T> Type<T> parameterizedType(ParameterizedType type,
			Map<String, Type<?>> actualTypeArguments) {
		@SuppressWarnings("unchecked")
		Class<T> rawType = (Class<T>) type.getRawType();
		return new Type<>(rawType,
				types(type.getActualTypeArguments(), actualTypeArguments));
	}

	public final Class<T> rawType;
	private final Type<?>[] params;

	/**
	 * Used to model upper bound wildcard types like <code>? extends Foo</code>
	 */
	private final boolean upperBound;

	private Type(boolean upperBound, Class<T> rawType, Type<?>[] parameters) {
		assert (rawType != null);
		this.rawType = primitiveAsWrapper(rawType);
		this.params = parameters;
		this.upperBound = upperBound;
	}

	private Type(Class<T> rawType, Type<?>[] parameters) {
		this(false, rawType, parameters);
	}

	private Type(Class<T> rawType) {
		this(false, rawType, new Type<?>[0]);
	}

	@Override
	public Type<T> type() {
		return this;
	}

	@Override
	public <E> Type<E> typed(Type<E> type) {
		return type;
	}

	@SuppressWarnings("unchecked")
	public <S> Type<? extends S> castTo(Type<S> supertype)
			throws ClassCastException {
		toSupertype(supertype);
		return (Type<S>) this;
	}

	public <S> Type<S> toSupertype(Type<S> supertype) {
		if (!isAssignableTo(supertype)) {
			throw new ClassCastException(
					"Cannot cast " + this + " to " + supertype);
		}
		return supertype;
	}

	public Type<? extends T> asUpperBound() {
		return upperBound(true);
	}

	public Type<? extends T> upperBound(boolean upperBound) {
		return this.upperBound == upperBound
			? this
			: new Type<>(upperBound, rawType, params);
	}

	public Type<? extends T> asExactType() {
		return upperBound(false);
	}

	@SuppressWarnings("unchecked")
	public Type<T[]> addArrayDimension() {
		Object proto = newArray(rawType, 0);
		return new Type<>(upperBound, (Class<T[]>) proto.getClass(), params);
	}

	public boolean equalTo(Type<?> other) {
		return this == other
			|| rawType == other.rawType && upperBound == other.upperBound
				&& arrayEquals(params, other.params, Type::equalTo);
	}

	@Override
	public int hashCode() {
		return rawType.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Type<?> && equalTo((Type<?>) obj);
	}

	/**
	 * @return in case of an array type the {@link Class#getComponentType()}
	 *         with the same type parameters as this type or otherwise this
	 *         type.
	 */
	@SuppressWarnings("unchecked")
	public <B> Type<?> baseType() {
		if (!rawType.isArray())
			return this;
		Class<?> baseType = rawType;
		while (baseType.isArray()) {
			baseType = baseType.getComponentType();
		}
		return new Type<>(upperBound, (Class<B>) baseType, params);
	}

	/**
	 * @return The actual type parameters (arguments).
	 */
	public Type<?>[] parameters() {
		return params.clone();
	}

	public Type<?> parameter(int index) {
		if (index < 0 || index >= rawType.getTypeParameters().length)
			throw new IndexOutOfBoundsException("The type " + this
				+ " has no type parameter at index: " + index);
		return isRawType() ? WILDCARD : params[index];
	}

	public boolean isAssignableTo(Type<?> other) {
		if (!other.rawType.isAssignableFrom(rawType))
			return false;
		if (!isParameterized() || other.isRawType())
			return true; //raw type is ok - no parameters to check
		if (other.rawType == rawType) // both have the same rawType
			return allParametersAreAssignableTo(other);
		@SuppressWarnings("unchecked")
		Class<? super T> commonRawType = (Class<? super T>) other.rawType;
		Type<?> asOther = supertype(commonRawType, this);
		return asOther.allParametersAreAssignableTo(other);
	}

	private boolean allParametersAreAssignableTo(Type<?> other) {
		return arrayEquals(params, other.params, Type::asParameterAssignableTo);
	}

	private boolean asParameterAssignableTo(Type<?> other) {
		if (rawType == other.rawType)
			return !isParameterized() || allParametersAreAssignableTo(other);
		return other.isUpperBound() && isAssignableTo(other.asExactType());
	}

	public boolean isInterface() {
		return rawType.isInterface();
	}

	public boolean isAbstract() {
		return Modifier.isAbstract(rawType.getModifiers());
	}

	/**
	 * @return true if this type describes the upper bound of the required types
	 *         (a wildcard generic).
	 */
	public boolean isUpperBound() {
		return upperBound;
	}

	/**
	 * @see #hasTypeParameter() To check if the {@link Class} defines type
	 *      parameter.
	 * @return true if the type {@link #hasTypeParameter()} and parameters are
	 *         given.
	 */
	public boolean isParameterized() {
		return params.length > 0;
	}

	/**
	 * @see #isParameterized() To check if actual type parameters are given.
	 * 
	 * @return true when the {@link Class} defines type parameters (generics).
	 */
	public boolean hasTypeParameter() {
		return rawType.getTypeParameters().length > 0;
	}

	public int arrayDimensions() {
		return arrayDimensions(rawType);
	}

	private static int arrayDimensions(Class<?> type) {
		return !type.isArray()
			? 0
			: 1 + arrayDimensions(type.getComponentType());
	}

	@Override
	public boolean moreQualiedThan(Type<?> other) {
		if (!rawType.isAssignableFrom(other.rawType))
			return true;
		if ((hasTypeParameter() && !isParameterized())
			|| (isUpperBound() && !other.isUpperBound()))
			return false; // equal or other is a subtype of this
		if ((other.hasTypeParameter() && !other.isParameterized())
			|| (!isUpperBound() && other.isUpperBound()))
			return true;
		if (rawType == other.rawType)
			return moreApplicableParametersThan(other);
		@SuppressWarnings("unchecked")
		Type<?> asOther = supertype(rawType, (Type<? extends T>) other);
		return moreApplicableParametersThan(asOther);
	}

	private boolean moreApplicableParametersThan(Type<?> other) {
		if (params.length == 1)
			return params[0].moreQualiedThan(other.params[0]);
		int morePrecise = 0;
		for (int i = 0; i < params.length; i++)
			if (params[i].moreQualiedThan(other.params[0]))
				morePrecise++;
		return morePrecise > params.length - morePrecise;
	}

	/**
	 * Example - <i>typeOf</i>
	 * 
	 * <pre>
	 * Map&lt;String,String&gt; =&gt; Map&lt;? extends String, ? extends String&gt;
	 * </pre>
	 * 
	 * @return A {@link Type} having all its type arguments
	 *         {@link #asUpperBound()}s. Use this to model &lt;?&gt; wildcard
	 *         generic.
	 */
	public Type<T> parametizedAsUpperBounds() {
		if (!isParameterized())
			return isRawType()
				? parametized(wildcards(rawType.getTypeParameters()))
				: this;
		if (areAllTypeParametersAreUpperBounds())
			return this;
		return new Type<>(upperBound, rawType,
				arrayMap(params, Type::asUpperBound));
	}

	/**
	 * @return true, in case this is a raw type - that is a generic type without
	 *         any generic type information available.
	 */
	public boolean isRawType() {
		return !isParameterized() && rawType.getTypeParameters().length > 0;
	}

	/**
	 * @return True when all type parameters are upper bounds.
	 */
	public boolean areAllTypeParametersAreUpperBounds() {
		return !arrayContains(params, p -> !p.isUpperBound());
	}

	public Type<T> parametized(Class<?>... typeParams) {
		return parametized(arrayMap(typeParams, Type.class, Type::raw));
	}

	public Type<T> parametized(Type<?>... params) {
		checkTypeParameters(params);
		return new Type<>(upperBound, rawType, params);
	}

	@Override
	public String toString() {
		return name(true);
	}

	void toString(StringBuilder b, boolean canonicalName) {
		if (isUpperBound()) {
			if (rawType == Object.class) {
				b.append("?");
				return;
			}
			b.append("? extends ");
		}
		String name = canonicalName
			? rawType.getCanonicalName()
			: rawType.getSimpleName();
		b.append(rawType.isArray()
			? name.substring(0, name.indexOf('['))
			: name);
		if (isParameterized()) {
			b.append('<');
			params[0].toString(b, canonicalName);
			for (int i = 1; i < params.length; i++) {
				b.append(',');
				params[i].toString(b, canonicalName);
			}
			b.append('>');
		}
		if (rawType.isArray()) {
			b.append(name.substring(name.indexOf('[')));
		}
	}

	public String simpleName() {
		return name(false);
	}

	private String name(boolean canonicalName) {
		StringBuilder b = new StringBuilder();
		toString(b, canonicalName);
		return b.toString();
	}

	private void checkTypeParameters(Type<?>... params) {
		if (params.length == 0)
			return; // is treated as raw-type
		if (arrayDimensions() > 0) {
			baseType().checkTypeParameters(params);
			return;
		}
		TypeVariable<Class<T>>[] vars = rawType.getTypeParameters();
		if (vars.length != params.length)
			throw new IllegalArgumentException(
					"Invalid nuber of type arguments - " + rawType
						+ " has type variables " + Arrays.toString(vars)
						+ " but got:" + Arrays.toString(params));
		for (int i = 0; i < vars.length; i++) {
			for (java.lang.reflect.Type t : vars[i].getBounds()) {
				Type<?> var = type(t, new HashMap<>());
				if (t != Object.class && !params[i].isAssignableTo(var)) {
					throw new IllegalArgumentException(params[i]
						+ " is not assignable to the type variable: " + var);
				}
			}
		}
	}

	public static <S> Type<? extends S> supertype(Class<S> supertype,
			Type<? extends S> type) {
		if (supertype.getTypeParameters().length == 0)
			return raw(supertype); // just for better performance 
		@SuppressWarnings("unchecked")
		Type<? extends S> res = (Type<? extends S>) arrayFindFirst(
				type.supertypes(), s -> s.rawType == supertype);
		if (res == null)
			throw new IllegalArgumentException("`" + supertype
				+ "` is not a supertype of: `" + type + "`");
		return res;
	}

	/**
	 * @return a list of all super-classes and super-interfaces of this type
	 *         starting with the direct super-class followed by the direct
	 *         super-interfaces continuing by going up the type hierarchy.
	 */
	public Type<? super T>[] supertypes() {
		Set<Type<?>> res = new LinkedHashSet<>();
		Class<?> supertype = rawType;
		java.lang.reflect.Type genericSupertype = null;
		Type<?> type = this;
		Map<String, Type<?>> actualTypeArguments = actualTypeArguments(type);
		if (!isInterface())
			res.add(OBJECT);
		while (supertype != null) {
			if (genericSupertype != null) {
				type = type(genericSupertype, actualTypeArguments);
				res.add(type);
			}
			actualTypeArguments = actualTypeArguments(type);
			addSuperInterfaces(res, supertype, actualTypeArguments);
			genericSupertype = supertype.getGenericSuperclass();
			supertype = supertype.getSuperclass();
		}
		@SuppressWarnings("unchecked")
		Type<? super T>[] supertypes = (Type<? super T>[]) res.toArray(
				new Type<?>[0]);
		return supertypes;
	}

	private static <V> Map<String, Type<?>> actualTypeArguments(Type<V> type) {
		Map<String, Type<?>> actualTypeArguments = new HashMap<>();
		TypeVariable<Class<V>>[] typeParams = type.rawType.getTypeParameters();
		for (int i = 0; i < typeParams.length; i++) {
			// it would be correct to use the joint type of the bounds but since it is not possible to create a type with illegal parameters it is ok to just use Object since there is no way to model a joint type
			actualTypeArguments.put(typeParams[i].getName(), type.parameter(i));
		}
		return actualTypeArguments;
	}

	private void addSuperInterfaces(Set<Type<?>> res, Class<?> type,
			Map<String, Type<?>> actualTypeArguments) {
		Class<?>[] interfaces = type.getInterfaces();
		java.lang.reflect.Type[] genericInterfaces = type.getGenericInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			Type<?> interfaceType = Type.type(genericInterfaces[i],
					actualTypeArguments);
			if (!res.contains(interfaceType)) {
				res.add(interfaceType);
				addSuperInterfaces(res, interfaces[i],
						actualTypeArguments(interfaceType));
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> primitiveAsWrapper(Class<T> type) {
		if (!type.isPrimitive())
			return type;
		if (type == int.class)
			return (Class<T>) Integer.class;
		if (type == boolean.class)
			return (Class<T>) Boolean.class;
		if (type == long.class)
			return (Class<T>) Long.class;
		if (type == char.class)
			return (Class<T>) Character.class;
		if (type == void.class)
			return (Class<T>) Void.class;
		if (type == float.class)
			return (Class<T>) Float.class;
		if (type == double.class)
			return (Class<T>) Double.class;
		if (type == byte.class)
			return (Class<T>) Byte.class;
		if (type == short.class)
			return (Class<T>) Short.class;
		throw new UnsupportedOperationException(
				"The primitive " + type + " cannot be wrapped yet!");
	}
}