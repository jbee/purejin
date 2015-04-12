/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.lang.reflect.Constructor;
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
 * A generic version of {@link Class} like {@link java.lang.reflect.Type} but without a complex
 * hierarchy. Instead all cases are represented as a general model. The key difference is that this
 * model just describes concrete types. So there is no representation for a {@link TypeVariable}.
 * 
 * There are some generic cases that are not supported right now because they haven't been needed.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Type<T>
		implements MorePreciseThan<Type<?>>, Parameter<T> {

	public static final Type<Object> OBJECT = Type.raw( Object.class );
	public static final Type<Void> VOID = raw( Void.class );
	public static final Type<? extends Object> WILDCARD = OBJECT.asUpperBound();

	public static Type<?> fieldType( Field field ) {
		return type( field.getGenericType() );
	}

	public static Type<?> returnType( Method method ) {
		return type( method.getGenericReturnType() );
	}

	public static Type<?>[] parameterTypes( Constructor<?> constructor ) {
		return parameterTypes( constructor.getGenericParameterTypes() );
	}

	public static Type<?>[] parameterTypes( Method method ) {
		return parameterTypes( method.getGenericParameterTypes() );
	}

	private static Type<?>[] parameterTypes( java.lang.reflect.Type[] genericParameterTypes ) {
		Type<?>[] res = new Type<?>[genericParameterTypes.length];
		for ( int i = 0; i < res.length; i++ ) {
			res[i] = type( genericParameterTypes[i] );
		}
		return res;
	}

	public static Type<?>[] wildcards( TypeVariable<?>... variables ) {
		Type<?>[] wildcards = new Type<?>[variables.length];
		Arrays.fill(wildcards, WILDCARD);
		return wildcards;
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<T> elementType( Class<T[]> arrayType ) {
		return (Type<T>) raw( arrayType ).baseType();
	}

	public static <T> Type<T> raw( Class<T> type ) {
		return new Type<T>( type );
	}

	private static UnsupportedOperationException notSupportedYet( java.lang.reflect.Type type ) {
		return new UnsupportedOperationException( "Type has no support yet: " + type );
	}

	private static Type<?>[] types( java.lang.reflect.Type[] parameters,
			Map<String, Type<?>> actualTypeArguments ) {
		Type<?>[] args = new Type<?>[parameters.length];
		for ( int i = 0; i < parameters.length; i++ ) {
			args[i] = type( parameters[i], actualTypeArguments );
		}
		return args;
	}

	private static Type<?> type( java.lang.reflect.Type type ) {
		return type( type, Collections.<String, Type<?>> emptyMap() );
	}

	private static Type<?> type( java.lang.reflect.Type type,
			Map<String, Type<?>> actualTypeArguments ) {
		if ( type instanceof Class<?> ) {
			return raw( (Class<?>) type );
		}
		if ( type instanceof ParameterizedType ) {
			return parameterizedType( (ParameterizedType) type, actualTypeArguments );
		}
		if ( type instanceof TypeVariable<?> ) {
			return actualTypeArguments.get( ( (TypeVariable<?>) type ).getName() );
		}
		if ( type instanceof GenericArrayType ) {
			GenericArrayType gat = (GenericArrayType) type;
			return type( gat.getGenericComponentType() ).addArrayDimension();
		}
		if ( type instanceof WildcardType ) {
			WildcardType wt = (WildcardType) type;
			java.lang.reflect.Type[] upperBounds = wt.getUpperBounds();
			if ( upperBounds.length == 1 ) {
				return type( upperBounds[0] ).asUpperBound();
			}
		}
		throw notSupportedYet( type );
	}

	private static <T> Type<T> parameterizedType( ParameterizedType type,
			Map<String, Type<?>> actualTypeArguments ) {
		@SuppressWarnings ( "unchecked" )
		Class<T> rawType = (Class<T>) type.getRawType();
		return new Type<T>( rawType, types( type.getActualTypeArguments(), actualTypeArguments ) );
	}

	public final Class<T> rawType;
	private final Type<?>[] params;

	/**
	 * Used to model upper bound wildcard types like <code>? extends Foo</code>
	 */
	private final boolean upperBound;

	private Type( boolean upperBound, Class<T> rawType, Type<?>[] parameters ) {
		assert ( rawType != null );
		this.rawType = primitiveAsWrapper( rawType );
		this.params = parameters;
		this.upperBound = upperBound;
	}

	private Type( Class<T> rawType, Type<?>[] parameters ) {
		this( false, rawType, parameters );
	}

	private Type( Class<T> rawType ) {
		this( false, rawType, new Type<?>[0] );
	}

	@Override
	public Type<T> type() {
		return this;
	}

	@Override
	public <E> Type<E> typed( Type<E> type ) {
		return type;
	}

	@SuppressWarnings ( "unchecked" )
	public <S> Type<? extends S> castTo( Type<S> supertype ) {
		if ( !isAssignableTo( supertype ) ) {
			throw new ClassCastException( "Cannot cast " + this + " to " + supertype );
		}
		return (Type<S>) this;
	}

	public Type<? extends T> asUpperBound() {
		return upperBound( true );
	}

	public Type<? extends T> upperBound( boolean upperBound ) {
		return new Type<T>( upperBound, rawType, params );
	}

	public Type<? extends T> asExactType() {
		return new Type<T>( false, rawType, params );
	}

	@SuppressWarnings ( "unchecked" )
	public Type<T[]> addArrayDimension() {
		Object proto = Array.newInstance( rawType, 0 );
		return new Type<T[]>( upperBound, (Class<T[]>) proto.getClass(), params );
	}

	public boolean equalTo( Type<?> other ) {
		if ( this == other ) {
			return true;
		}
		if ( rawType != other.rawType ) {
			return false;
		}
		if ( params.length != other.params.length ) {
			return false;
		}
		for ( int i = 0; i < params.length; i++ ) {
			if ( !params[i].equalTo( other.params[i] ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return rawType.hashCode();
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Type<?> && equalTo( (Type<?>) obj );
	}

	/**
	 * @return in case of an array type the {@link Class#getComponentType()} with the same type
	 *         parameters as this type or otherwise this type.
	 */
	@SuppressWarnings("unchecked")
	public <B> Type<?> baseType() {
		if (!rawType.isArray())
			return this;
		Class<?> baseType = rawType;
		while (baseType.isArray()) {
			baseType = baseType.getComponentType();
		}
		return new Type<B>( upperBound, (Class<B>)baseType, params );
	}

	/**
	 * @return The actual type parameters (arguments).
	 */
	public Type<?>[] parameters() {
		return params.clone();
	}

	public Type<?> parameter( int index ) {
		if ( index < 0 || index >= rawType.getTypeParameters().length ) {
			throw new IndexOutOfBoundsException( "The type " + this
					+ " has no type parameter at index: " + index );
		}
		return isRawType()
			? WILDCARD
			: params[index];
	}

	@Override
	public boolean isAssignableTo( Type<?> other ) {
		if ( !other.rawType.isAssignableFrom( rawType ) ) {
			return false;
		}
		if ( !isParameterized() || other.isRawType() ) {
			return true; //raw type is ok - no parameters to check
		}
		if ( other.rawType == rawType ) { // both have the same rawType
			return allParametersAreAssignableTo( other );
		}
		@SuppressWarnings ( "unchecked" )
		Class<? super T> commonRawType = (Class<? super T>) other.rawType;
		Type<?> asOther = supertype( commonRawType, this );
		return asOther.allParametersAreAssignableTo( other );
	}

	private boolean allParametersAreAssignableTo( Type<?> other ) {
		for ( int i = 0; i < params.length; i++ ) {
			if ( !params[i].asParameterAssignableTo( other.params[i] ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean asParameterAssignableTo( Type<?> other ) {
		if ( rawType == other.rawType ) {
			return !isParameterized() || allParametersAreAssignableTo( other );
		}
		return other.isUpperBound() && isAssignableTo( other.asExactType() );
	}

	public boolean isInterface() {
		return rawType.isInterface();
	}

	public boolean isAbstract() {
		return Modifier.isAbstract( rawType.getModifiers() );
	}

	/**
	 * @return true if this type describes the upper bound of the required types (a wildcard
	 *         generic).
	 */
	public boolean isUpperBound() {
		return upperBound;
	}

	/**
	 * @see #hasTypeParameter() To check if the {@link Class} defines type parameter.
	 * @return true if the type {@link #hasTypeParameter()} and parameters are given.
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
		return arrayDimensions( rawType );
	}

	private static int arrayDimensions( Class<?> type ) {
		return !type.isArray()
			? 0
			: 1 + arrayDimensions( type.getComponentType() );
	}

	@Override
	public boolean morePreciseThan( Type<?> other ) {
		if ( !rawType.isAssignableFrom( other.rawType ) ) {
			return true;
		}
		if ( ( hasTypeParameter() && !isParameterized() )
				|| ( isUpperBound() && !other.isUpperBound() ) ) {
			return false; // equal or other is a subtype of this
		}
		if ( ( other.hasTypeParameter() && !other.isParameterized() )
				|| ( !isUpperBound() && other.isUpperBound() ) ) {
			return true;
		}
		if ( rawType == other.rawType ) {
			return morePreciseParametersThan( other );
		}
		@SuppressWarnings ( "unchecked" )
		Type<?> asOther = supertype( rawType, (Type<? extends T>) other );
		return morePreciseParametersThan( asOther );
	}

	private boolean morePreciseParametersThan( Type<?> other ) {
		if ( params.length == 1 ) {
			return params[0].morePreciseThan( other.params[0] );
		}
		int morePrecise = 0;
		for ( int i = 0; i < params.length; i++ ) {
			if ( params[i].morePreciseThan( other.params[0] ) ) {
				morePrecise++;
			}
		}
		return morePrecise > params.length - morePrecise;
	}

	/**
	 * Example - <i>typeOf</i>
	 * 
	 * <pre>
	 * Map&lt;String,String&gt; =&gt; Map&lt;? extends String, ? extends String&gt;
	 * </pre>
	 * 
	 * @return A {@link Type} having all its type arguments {@link #asUpperBound()}s. Use this to
	 *         model &lt;?&gt; wildcard generic.
	 */
	public Type<T> parametizedAsUpperBounds() {
		if ( !isParameterized() ) {
			if ( isRawType() ) {
				return parametized( wildcards( rawType.getTypeParameters() ) );
			}
			return this;
		}
		if ( areAllTypeParametersAreUpperBounds() ) {
			return this;
		}
		Type<?>[] parameters = new Type<?>[params.length];
		for ( int i = 0; i < params.length; i++ ) {
			parameters[i] = params[i].asUpperBound();
		}
		return new Type<T>( upperBound, rawType, parameters );
	}

	/**
	 * @return true, in case this is a raw type - that is a generic type without any generic type
	 *         information available.
	 */
	public boolean isRawType() {
		return !isParameterized() && rawType.getTypeParameters().length > 0;
	}

	/**
	 * @return True when all type parameters are upper bounds.
	 */
	public boolean areAllTypeParametersAreUpperBounds() {
		for ( int i = 0; i < params.length; i++ ) {
			if ( !params[i].isUpperBound() ) {
				return false;
			}
		}
		return true;
	}

	public Type<T> parametized( Class<?>... arguments ) {
		Type<?>[] typeArgs = new Type<?>[arguments.length];
		for ( int i = 0; i < arguments.length; i++ ) {
			typeArgs[i] = raw( arguments[i] );
		}
		return parametized( typeArgs );
	}

	public Type<T> parametized( Type<?>... parameters ) {
		checkTypeParameters( parameters );
		return new Type<T>( upperBound, rawType, parameters );
	}

	@Override
	public String toString() {
		return name(true);
	}

	void toString( StringBuilder b, boolean canonicalName ) {
		if ( isUpperBound() ) {
			if ( rawType == Object.class ) {
				b.append( "?" );
				return;
			}
			b.append( "? extends " );
		}
		String name = canonicalName
			? rawType.getCanonicalName()
			: rawType.getSimpleName();
		b.append( rawType.isArray()
			? name.substring( 0, name.indexOf( '[' ) )
			: name );
		if ( isParameterized() ) {
			b.append( '<' );
			params[0].toString( b, canonicalName );
			for ( int i = 1; i < params.length; i++ ) {
				b.append( ',' );
				params[i].toString( b, canonicalName );
			}
			b.append( '>' );
		}
		if ( rawType.isArray() ) {
			b.append( name.substring( name.indexOf( '[' ) ) );
		}
	}

	public String simpleName() {
		return name(false);
	}

	private String name(boolean canonicalName) {
		StringBuilder b = new StringBuilder();
		toString( b, canonicalName );
		return b.toString();
	}

	private void checkTypeParameters( Type<?>... parameters ) {
		if ( parameters.length == 0 ) {
			return; // is treated as raw-type
		}
		if ( arrayDimensions() > 0 ) {
			baseType().checkTypeParameters( parameters );
			return;
		}
		TypeVariable<Class<T>>[] vars = rawType.getTypeParameters();
		if ( vars.length != parameters.length ) {
			throw new IllegalArgumentException( "Invalid nuber of type arguments - " + rawType
					+ " has type variables " + Arrays.toString( vars ) + " but got:"
					+ Arrays.toString( parameters ) );
		}
		for ( int i = 0; i < vars.length; i++ ) {
			for ( java.lang.reflect.Type t : vars[i].getBounds() ) {
				Type<?> vt = type( t, new HashMap<String, Type<?>>() );
				if ( t != Object.class && !parameters[i].isAssignableTo( vt ) ) {
					throw new IllegalArgumentException( parameters[i]
							+ " is not assignable to the type variable: " + vt );
				}
			}
		}
	}

	@SuppressWarnings ( "unchecked" )
	public static <S> Type<? extends S> supertype( Class<S> supertype, Type<? extends S> type ) {
		if ( supertype.getTypeParameters().length == 0 ) {
			return raw( supertype ); // just for better performance 
		}
		for ( Type<?> s : type.supertypes() ) {
			if ( s.rawType == supertype ) {
				return (Type<? extends S>) s;
			}
		}
		throw new IllegalArgumentException( "`" + supertype + "` is not a supertype of: `" + type + "`" );
	}

	/**
	 * @return a list of all super-classes and super-interfaces of this type starting with the
	 *         direct super-class followed by the direct super-interfaces continuing by going up the
	 *         type hierarchy.
	 */
	public Type<? super T>[] supertypes() {
		Set<Type<?>> res = new LinkedHashSet<Type<?>>();
		Class<?> supertype = rawType;
		java.lang.reflect.Type genericSupertype = null;
		Type<?> type = this;
		Map<String, Type<?>> actualTypeArguments = actualTypeArguments( type );
		if ( !isInterface() ) {
			res.add( OBJECT );
		}
		while ( supertype != null ) {
			if ( genericSupertype != null ) {
				type = type( genericSupertype, actualTypeArguments );
				res.add( type );
			}
			actualTypeArguments = actualTypeArguments( type );
			addSuperInterfaces( res, supertype, actualTypeArguments );
			genericSupertype = supertype.getGenericSuperclass();
			supertype = supertype.getSuperclass();
		}
		@SuppressWarnings ( "unchecked" )
		Type<? super T>[] supertypes = (Type<? super T>[]) res.toArray( new Type<?>[res.size()] );
		return supertypes;
	}

	private static <V> Map<String, Type<?>> actualTypeArguments( Type<V> type ) {
		Map<String, Type<?>> actualTypeArguments = new HashMap<String, Type<?>>();
		TypeVariable<Class<V>>[] typeParameters = type.rawType.getTypeParameters();
		for ( int i = 0; i < typeParameters.length; i++ ) {
			// it would be correct to use the joint type of the bounds but since it is not possible to create a type with illegal parameters it is ok to just use Object since there is no way to model a joint type
			actualTypeArguments.put( typeParameters[i].getName(), type.parameter( i ) );
		}
		return actualTypeArguments;
	}

	private void addSuperInterfaces( Set<Type<?>> res, Class<?> type, Map<String, Type<?>> actualTypeArguments ) {
		Class<?>[] interfaces = type.getInterfaces();
		java.lang.reflect.Type[] genericInterfaces = type.getGenericInterfaces();
		for ( int i = 0; i < interfaces.length; i++ ) {
			Type<?> interfaceType = Type.type( genericInterfaces[i], actualTypeArguments );
			if ( !res.contains( interfaceType ) ) {
				res.add( interfaceType );
				addSuperInterfaces( res, interfaces[i], actualTypeArguments( interfaceType ) );
			}
		}
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Class<T> primitiveAsWrapper( Class<T> primitive ) {
		if ( !primitive.isPrimitive() ) {
			return primitive;
		}
		if ( primitive == int.class ) {
			return (Class<T>) Integer.class;
		}
		if ( primitive == boolean.class ) {
			return (Class<T>) Boolean.class;
		}
		if ( primitive == long.class ) {
			return (Class<T>) Long.class;
		}
		if ( primitive == char.class ) {
			return (Class<T>) Character.class;
		}
		if ( primitive == void.class ) {
			return (Class<T>) Void.class;
		}
		if ( primitive == float.class ) {
			return (Class<T>) Float.class;
		}
		if ( primitive == double.class ) {
			return (Class<T>) Double.class;
		}
		if ( primitive == byte.class ) {
			return (Class<T>) Byte.class;
		}
		if ( primitive == short.class ) {
			return (Class<T>) Short.class;
		}
		throw new UnsupportedOperationException( "The primitive " + primitive + " cannot be wrapped yet!" );
	}
}