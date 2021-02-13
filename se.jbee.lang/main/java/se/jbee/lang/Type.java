/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.lang;

import java.io.Serializable;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * A generic version of {@link Class} like {@link java.lang.reflect.Type} but
 * without a complex hierarchy. Instead all cases are represented as a general
 * model. The key difference is that this model just describes actual types.
 * There is no representation for a {@link TypeVariable}.
 *
 * Lower bound types ({@code ? super X}) are not supported as they usually are
 * not needed in context of injection.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@SuppressWarnings({ "squid:S1448", "squid:S1200" })
public final class Type<T> implements Qualifying<Type<?>>, Typed<T>,
		Serializable, Comparable<Type<?>> {

	public static final Type<Object> OBJECT = Type.raw(Object.class);
	public static final Type<Void> VOID = raw(Void.class);
	public static final Type<?> WILDCARD = OBJECT.asUpperBound();
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final Type<Class<?>> CLASS = (Type) classType(Class.class);

	public static <T> Type<T> classType(Class<T> type) {
		Class<?> base = type;
		while (base.isArray()) {
			base = base.getComponentType();
		}
		if (base != type) {
			Type<?> genericBase = raw(base).varTypeParametersAsWildcards();
			return new Type<>(type, genericBase.params);
		}
		return raw(type).varTypeParametersAsWildcards();
	}

	@SuppressWarnings("unchecked")
	public static <T> Type<? extends T> actualInstanceType(T instance, Type<?> as) {
		Class<?> rawType = instance.getClass();
		if (as.rawType == rawType)
			return (Type<? extends T>) as;
		TypeVariable<? extends Class<?>>[] generics = rawType.getTypeParameters();
		if (generics.length == 0)
			return (Type<? extends T>) raw(rawType);
		throw new UnsupportedOperationException("Not supported yet");
	}

	public static int typeVariableComparator(TypeVariable<?> a, TypeVariable<?> b) {
		int res = a.getName().compareTo(b.getName());
		if (res != 0)
			return res;
		if (a.getGenericDeclaration().equals(b.getGenericDeclaration()))
			return 0;
		return Integer.compare(a.getGenericDeclaration().hashCode(), b.getGenericDeclaration().hashCode());
	}

	private Type<T> varTypeParametersAsWildcards() {
		TypeVariable<?>[] vars = rawType.getTypeParameters();
		return vars.length == 0
				? this
				: parameterized(actualTypes(vars, emptyTypeArguments()));
	}

	public static Type<?> actualFieldType(Field member, Type<?> genericDeclaringClass) {
		return genericType(member.getGenericType(), actualTypeArguments(genericDeclaringClass));
	}

	public static Type<?> actualReturnType(Method member, Type<?> genericDeclaringClass) {
		return genericType(member.getGenericReturnType(), actualTypeArguments(genericDeclaringClass));
	}

	public static Type<?> actualReturnType(Constructor<?> member, Type<?> genericDeclaringClass) {
		return genericType(member.getAnnotatedReturnType().getType(), actualTypeArguments(genericDeclaringClass));
	}

	public static Type<?> actualParameterType(java.lang.reflect.Parameter param, Type<?> genericDeclaringClass) {
		return genericType(param.getParameterizedType(), actualTypeArguments(genericDeclaringClass));
	}

	public static Type<?> fieldType(Field field) {
		return genericType(field.getGenericType());
	}

	/**
	 * Returns the generic {@link Method} return {@link Type} with any {@link
	 * Class} level type parameter replaced with its lower bound, so {@code ?}
	 * for an unbound type parameter or {@code Serializable} for {@code ?
	 * extends Serializable}.
	 *
	 * @param method any {@link Method}
	 * @return The fully generic return {@link Type} (filling class level type
	 * parameters with lower bounds)
	 */
	public static Type<?> returnType(Method method) {
		return genericType(method.getGenericReturnType());
	}

	public static Type<?> parameterType(java.lang.reflect.Parameter param) {
		return genericType(param.getParameterizedType());
	}

	public static Type<?>[] parameterTypes(Executable methodOrConstructor) {
		return parameterTypes(methodOrConstructor.getGenericParameterTypes());
	}

	private static Type<?>[] parameterTypes(
			java.lang.reflect.Type[] genericParameterTypes) {
		return Utils.arrayMap(genericParameterTypes, Type.class, Type::genericType);
	}

	public static Type<?>[] actualTypes(TypeVariable<?>[] vars,
			Map<TypeVariable<?>, Type<?>> actualTypeArguments) {
		Type<?>[] actualTypes = new Type<?>[vars.length];
		for (int i = 0; i < vars.length; i++)
			actualTypes[i] = actualType(vars[i], actualTypeArguments);
		return actualTypes;
	}

	private static Type<?> actualType(TypeVariable<?> var,
			Map<TypeVariable<?>, Type<?>> actualTypeArguments) {
		Type<?> actualType = actualTypeArguments.get(var);
		if (actualType != null)
			return actualType;
		if (actualTypeArguments.containsKey(var)) // we have or are in the process of computing that variable
			return Type.WILDCARD;
		actualTypeArguments.put(var, null); // not yet have it but we want to stop looking it up again so we mark it by putting a null for the name
		actualType = var.getBounds().length == 1
				? Type.genericType(var.getBounds()[0], actualTypeArguments).asUpperBound()
				: WILDCARD;
		// now we actually have it and add it...
		actualTypeArguments.put(var, actualType);
		return actualType;
	}

	public static <T> Type<T> raw(Class<T> type) {
		return new Type<>(type);
	}

	private static Type<?>[] genericTypes(java.lang.reflect.Type[] types,
			Map<TypeVariable<?>, Type<?>> actualTypeArguments) {
		if (types.length == 0)
			return new Type[0];
		return Utils.arrayMap(types, Type.class,
				p -> genericType(p, actualTypeArguments));
	}

	private static Type<?> genericType(java.lang.reflect.Type type) {
		return genericType(type, emptyTypeArguments());
	}

	@SuppressWarnings("ChainOfInstanceofChecks")
	public static Type<?> genericType(java.lang.reflect.Type type,
			Map<TypeVariable<?>, Type<?>> actualTypeArguments) {
		if (type instanceof Class<?>)
			return classType((Class<?>) type);
		if (type instanceof ParameterizedType)
			return genericType((ParameterizedType) type,
					actualTypeArguments);
		if (type instanceof TypeVariable<?>)
			return actualType((TypeVariable<?>) type, actualTypeArguments);
		if (type instanceof GenericArrayType) {
			GenericArrayType gat = (GenericArrayType) type;
			return genericType(gat.getGenericComponentType(),
					actualTypeArguments).addArrayDimension();
		}
		if (type instanceof WildcardType) {
			WildcardType wt = (WildcardType) type;
			java.lang.reflect.Type[] upperBounds = wt.getUpperBounds();
			if (upperBounds.length == 1)
				return genericType(upperBounds[0], actualTypeArguments).asUpperBound();
		}
		throw new UnsupportedOperationException(
				"Type has no support yet: " + type);
	}

	private static <T> Type<T> genericType(ParameterizedType type,
			Map<TypeVariable<?>, Type<?>> actualTypeArguments) {
		@SuppressWarnings("unchecked")
		Class<T> rawType = (Class<T>) type.getRawType();
		java.lang.reflect.Type[] typeArguments = type.getActualTypeArguments();
		if (typeArguments.length == 1)
			return new Type<>(rawType, new Type[] {
					genericType(typeArguments[0], actualTypeArguments) });
		return new Type<>(rawType,
				genericTypes(typeArguments, actualTypeArguments));
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
	public <S> Type<? extends S> castTo(Type<S> supertype) {
		if (!isAssignableTo(supertype))
			throw new ClassCastException(
					"Cannot cast " + this + " to " + supertype);
		return (Type<S>) this;
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
		Object proto = Utils.newArray(rawType, 0);
		return new Type<>(upperBound, (Class<T[]>) proto.getClass(), params);
	}

	public boolean equalTo(Type<?> other) {
		return this == other
			|| rawType == other.rawType && upperBound == other.upperBound
				&& Utils.arrayEquals(params, other.params, Type::equalTo);
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
		while (baseType.isArray())
			baseType = baseType.getComponentType();
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
		return toSuperType(other.rawType).allParametersAreAssignableTo(other);
	}

	private boolean allParametersAreAssignableTo(Type<?> other) {
		return Utils.arrayEquals(params, other.params, Type::asParameterAssignableTo);
	}

	public boolean asParameterAssignableTo(Type<?> other) {
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
	 * @return true if this {@link Type#isParameterized()} and any of its
	 *         parameters {@link #isUpperBound()}
	 */
	public boolean isParameterizedAsUpperBound() {
		return isParameterized() && Utils.arrayContains(params,
				p -> p.isUpperBound() || p.isParameterizedAsUpperBound());
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
	public boolean moreQualifiedThan(Type<?> other) {
		if (!rawType.isAssignableFrom(other.rawType))
			return true;
		if ((hasTypeParameter() && !isParameterized())
			|| (isUpperBound() && !other.isUpperBound()))
			return false; // equal or other is a subtype of this
		if ((other.hasTypeParameter() && !other.isParameterized())
			|| (!isUpperBound() && other.isUpperBound()))
			return true;
		if (rawType == other.rawType)
			return moreQualifiedParametersThan(other);
		return moreQualifiedParametersThan(other.toSuperType(rawType));
	}

	private boolean moreQualifiedParametersThan(Type<?> other) {
		if (params.length == 1)
			return params[0].moreQualifiedThan(other.params[0]);
		int moreQualified = 0;
		for (Type<?> param : params)
			if (param.moreQualifiedThan(other.params[0]))
				moreQualified++;
		return moreQualified > params.length - moreQualified;
	}

	@Override
	public int compareTo(Type<?> other) {
		int res = rawType.getName().compareTo(other.rawType.getName());
		if (res != 0)
			return res;
		res = Boolean.compare(upperBound, other.upperBound);
		if (res != 0)
			return res;
		return Utils.arrayCompare(params, other.params);
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
	public Type<T> parameterizedAsUpperBounds() {
		if (!isParameterized())
			return isRawType()
				? parameterized(actualTypes(rawType.getTypeParameters(),
					emptyTypeArguments()))
				: this;
		if (allTypeParametersAreUpperBounds())
			return this;
		return new Type<>(upperBound, rawType,
				Utils.arrayMap(params, Type::asUpperBound));
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
	public boolean allTypeParametersAreUpperBounds() {
		return !Utils.arrayContains(params, p -> !p.isUpperBound());
	}

	public Type<T> parameterized(Class<?>... typeParams) {
		return parameterized(Utils.arrayMap(typeParams, Type.class, Type::raw));
	}

	public Type<T> parameterized(Type<?>... params) {
		checkTypeParameters(params);
		return new Type<>(upperBound, rawType, params);
	}

	@Override
	public String toString() {
		return name(true);
	}

	private void toString(StringBuilder str, boolean fullName) {
		if (isUpperBound()) {
			if (rawType == Object.class) {
				str.append("?");
				return;
			}
			str.append("? extends ");
		}
		String name = fullName
			? rawType.getCanonicalName()
			: rawType.getSimpleName();
		if (name == null) // fallback for anonymous inner classes which have no canonical or simple name
			name = rawType.getName();
		str.append(rawType.isArray()
			? name.substring(0, name.indexOf('['))
			: name);
		if (isParameterized()) {
			str.append('<');
			params[0].toString(str, fullName);
			for (int i = 1; i < params.length; i++) {
				str.append(',');
				params[i].toString(str, fullName);
			}
			str.append('>');
		}
		if (rawType.isArray())
			str.append(name.substring(name.indexOf('[')));
	}

	public String simpleName() {
		return name(false);
	}

	private String name(boolean canonicalName) {
		StringBuilder str = new StringBuilder();
		toString(str, canonicalName);
		return str.toString();
	}

	private void checkTypeParameters(Type<?>... args) {
		if (args.length == 0)
			return; // is treated as raw-type
		if (arrayDimensions() > 0) {
			baseType().checkTypeParameters(args);
			return;
		}
		TypeVariable<Class<T>>[] vars = rawType.getTypeParameters();
		if (vars.length != args.length)
			throw new IllegalArgumentException(
					"Invalid number of type arguments - " + rawType
						+ " has type variables " + Arrays.toString(vars)
						+ " but got:" + Arrays.toString(args));
		Map<TypeVariable<?>, Type<?>> actualTypeArguments = emptyTypeArguments();
		for (int i = 0; i < vars.length; i++)
			checkTypeParameters(vars[i], args[i], actualTypeArguments);
	}

	private void checkTypeParameters(TypeVariable<Class<T>> var, Type<?> arg,
			Map<TypeVariable<?>, Type<?>> actualTypeArguments) {
		for (java.lang.reflect.Type genericUpperBound : var.getBounds()) {
			Type<?> upperBoundType = genericType(genericUpperBound, actualTypeArguments);
			Type<?> varType = actualTypeArguments.get(var);
			if (varType != null && arg.equalTo(varType))
				return; // recursive definition
			if (genericUpperBound != Object.class && !arg.isAssignableTo(upperBoundType)) {
				throw new IllegalArgumentException(arg
					+ " is not assignable to the type variable: " + upperBoundType);
			}
		}
	}

	/**
	 * Returns the actual super-type of this {@link Type} (including its actual
	 * type parameters) for the given super-{@link Class}.
	 *
	 * @param rawSuperType a {@link Class} reference to a super-class or
	 *                     super-interface of this {@link Type}. In other words
	 *                     a {@link Class} type this {@link Type} would be
	 *                     assignable to.
	 * @return the actual super-{@link Type}
	 * @throws ClassCastException if the given {@link Class} is no super-type of
	 *                            this {@link Type}
	 * @see #castTo(Type)
	 */
	@SuppressWarnings("unchecked")
	public Type<? super T> toSuperType(Class<?> rawSuperType) {
		/*
		This method has a central role as it is part of checking assignability.
		Therefore we do make have some short paths in case we can avoid walking
		the inheritance tree.
		 */
		if (rawSuperType == rawType)
			return this;
		if (rawSuperType.getTypeParameters().length == 0) {
			if (rawSuperType.isAssignableFrom(rawType))
				return (Type<? super T>) raw(rawSuperType);
			failedCastTo(rawSuperType);
		}
		Object[] box = new Object[1];
		walkSuperTypes(this, !rawSuperType.isInterface(),
				rawSuperType.isInterface(), true, t -> {
			boolean found = t.rawType == rawSuperType;
			if (found) box[0] = t;
			return !found;
		});
		if (box[0] == null)
			failedCastTo(rawSuperType);
		return (Type<? super T>) box[0];
	}

	private void failedCastTo(Class<?> rawSuperType) {
		throw new ClassCastException(
				"The type " + this + " does not have a super-type: " + rawSuperType);
	}

	/**
	 * @return a list of all super-classes and super-interfaces of this type
	 *         starting with the direct super-class followed by the direct
	 *         super-interfaces continuing by going up the type hierarchy.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Set<Type<? super T>> supertypes() {
		Set<Type<?>> supertypes = new LinkedHashSet<>();
		walkSuperTypes(this, true, true, false, supertypes::add);
		return (Set) supertypes;
	}

	/**
	 * Walks the inheritance tree and passes all actual types implemented by
	 * given {@code type} to the consumer.
	 *
	 * @return true, if the walking was cancelled by the consumer returning
	 * false and while cancelOnFalse is true.
	 */
	private static boolean walkSuperTypes(Type<?> type,
			boolean walkClassTypes, boolean walkInterfaceTypes,
			boolean cancelOnFalse, Predicate<Type<?>> consumer) {
		Class<?> supertype = type.rawType.getSuperclass();
		Map<TypeVariable<?>, Type<?>> actualTypeArguments = actualTypeArguments(
				type);
		if (walkInterfaceTypes && !walkSuperInterfaces(type.rawType, actualTypeArguments,
				cancelOnFalse, consumer))
			return false;
		if (supertype == null)
			return true; // done
		Type<?> superclass = genericType(type.rawType.getGenericSuperclass(),
				actualTypeArguments);
		if (walkClassTypes && !consumer.test(superclass) && cancelOnFalse)
			return false;
		return walkSuperTypes(superclass, walkClassTypes, walkInterfaceTypes,
				cancelOnFalse, consumer);
	}

	/**
	 * Walks the inheritance tree and passes all actual types implemented by
	 * given {@code type} to the consumer.
	 *
	 * @return true, if the walking was cancelled by the consumer returning
	 * false and while cancelOnFalse is true.
	 */
	private static boolean walkSuperInterfaces(Class<?> type,
			Map<TypeVariable<?>, Type<?>> actualTypeArguments,
			boolean cancelOnFalse, Predicate<Type<?>> consumer) {
		Class<?>[] ix = type.getInterfaces();
		java.lang.reflect.Type[] gix = type.getGenericInterfaces();
		for (int i = 0; i < ix.length; i++) {
			Type<?> interfaceType = Type.genericType(gix[i],
					actualTypeArguments);
			boolean res = consumer.test(interfaceType)
					&& walkSuperInterfaces(ix[i],
					actualTypeArguments(interfaceType), cancelOnFalse, consumer);
			if (!res && cancelOnFalse)
				return false;
		}
		return true;
	}

	public Map<TypeVariable<?>, Type<?>> actualTypeArguments() {
		return actualTypeArguments(this);
	}

	private static <V> Map<TypeVariable<?>, Type<?>> actualTypeArguments(Type<V> type) {
		Map<TypeVariable<?>, Type<?>> actualTypeArguments = emptyTypeArguments();
		TypeVariable<Class<V>>[] vars = type.rawType.getTypeParameters();
		for (int i = 0; i < vars.length; i++) {
			// it would be correct to use the joint type of the bounds but since it is not possible to create a type with illegal parameters it is ok to just use Object since there is no way to model a joint type
			actualTypeArguments.put(vars[i], type.parameter(i));
		}
		return actualTypeArguments;
	}

	public static Map<TypeVariable<?>, Type<?>> emptyTypeArguments() {
		return new TreeMap<>(Type::typeVariableComparator);
	}

	@SuppressWarnings({ "unchecked", "squid:S1541", "ChainOfInstanceofChecks" })
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
