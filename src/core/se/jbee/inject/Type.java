/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
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
		implements PreciserThan<Type<?>>, Parameter<T> {

	public static final Type<Object> OBJECT = Type.raw( Object.class );
	public static final Type<Void> VOID = raw( Void.class );
	public static final Type<? extends Object> WILDCARD = OBJECT.asLowerBound();

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
		Type<?>[] res = new Type<?>[variables.length];
		for ( int i = 0; i < res.length; i++ ) {
			res[i] = WILDCARD;
		}
		return res;
	}

	@SuppressWarnings ( "unchecked" )
	public static <T> Type<T> elementType( Class<T[]> arrayType ) {
		return (Type<T>) raw( arrayType ).elementType();
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
			return type( gat.getGenericComponentType() ).getArrayType();
		}
		throw notSupportedYet( type );
	}

	private static <T> Type<T> parameterizedType( ParameterizedType type,
			Map<String, Type<?>> actualTypeArguments ) {
		@SuppressWarnings ( "unchecked" )
		Class<T> rawType = (Class<T>) type.getRawType();
		return new Type<T>( rawType, types( type.getActualTypeArguments(), actualTypeArguments ) );
	}

	private final Class<T> rawType;
	private final Type<?>[] params;

	/**
	 * Used to model lower bound wildcard types like <code>? extends Foo</code>
	 */
	private final boolean lowerBound;

	private Type( boolean lowerBound, Class<T> rawType, Type<?>[] parameters ) {
		assert ( rawType != null );
		this.rawType = primitiveAsWrapper( rawType );
		this.params = parameters;
		this.lowerBound = lowerBound;
	}

	private Type( Class<T> rawType, Type<?>[] parameters ) {
		this( false, rawType, parameters );
	}

	private Type( Class<T> rawType ) {
		this( false, rawType, new Type<?>[0] );
	}

	public Type<? extends T> asLowerBound() {
		return lowerBound( true );
	}

	public Type<? extends T> lowerBound( boolean lowerBound ) {
		return new Type<T>( lowerBound, rawType, params );
	}

	public Type<? extends T> asExactType() {
		return new Type<T>( false, rawType, params );
	}

	@SuppressWarnings ( "unchecked" )
	public Type<T[]> getArrayType() {
		Object proto = Array.newInstance( rawType, 0 );
		return new Type<T[]>( lowerBound, (Class<T[]>) proto.getClass(), params );
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
	public Type<?> elementType() {
		Type<?> elemRawType = elementRawType();
		return elemRawType == this
			? this
			: elemRawType.parametized( params );
	}

	/**
	 * @return in case of an array type the {@link Class#getComponentType()} otherwise this type.
	 */
	private Type<?> elementRawType() {
		return asElementRawType( rawType.getComponentType() );
	}

	private <E> Type<?> asElementRawType( Class<E> elementType ) {
		return rawType.isArray()
			? new Type<E>( lowerBound, elementType, params )
			: this;
	}

	public Class<T> getRawType() {
		return rawType;
	}

	/**
	 * @return The actual type parameters (arguments).
	 */
	public Type<?>[] getParameters() {
		return params;
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
		Class<? super T> commonRawType = (Class<? super T>) other.getRawType();
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
		return other.isLowerBound() && isAssignableTo( other.asExactType() );
	}

	public boolean isInterface() {
		return rawType.isInterface();
	}

	public boolean isAbstract() {
		return Modifier.isAbstract( rawType.getModifiers() );
	}

	/**
	 * @return true if this type describes the lower bound of the required types (a wildcard
	 *         generic).
	 */
	public boolean isLowerBound() {
		return lowerBound;
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

	public boolean isUnidimensionalArray() {
		return rawType.isArray() && !rawType.getComponentType().isArray();
	}

	@Override
	public boolean morePreciseThan( Type<?> other ) {
		if ( !rawType.isAssignableFrom( other.rawType ) ) {
			return true;
		}
		if ( ( hasTypeParameter() && !isParameterized() )
				|| ( isLowerBound() && !other.isLowerBound() ) ) {
			return false; // equal or other is a subtype of this
		}
		if ( ( other.hasTypeParameter() && !other.isParameterized() )
				|| ( !isLowerBound() && other.isLowerBound() ) ) {
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
	 * @return A {@link Type} having all its type arguments {@link #asLowerBound()}s. Use this to
	 *         model &lt;?&gt; wildcard generic.
	 */
	public Type<T> parametizedAsLowerBounds() {
		if ( !isParameterized() ) {
			if ( isRawType() ) {
				return parametized( wildcards( rawType.getTypeParameters() ) );
			}
			return this;
		}
		if ( allArgumentsAreLowerBounds() ) {
			return this;
		}
		Type<?>[] parameters = new Type<?>[params.length];
		for ( int i = 0; i < params.length; i++ ) {
			parameters[i] = params[i].asLowerBound();
		}
		return new Type<T>( lowerBound, rawType, parameters );
	}

	/**
	 * @return true, in case this is a raw type - that is a generic type without any generic type
	 *         information available.
	 */
	public boolean isRawType() {
		return !isParameterized() && rawType.getTypeParameters().length > 0;
	}

	/**
	 * @return True when all type parameters are lower bounds.
	 */
	public boolean allArgumentsAreLowerBounds() {
		int c = 0;
		for ( int i = 0; i < params.length; i++ ) {
			if ( params[i].isLowerBound() ) {
				c++;
			}
		}
		return c == params.length;
	}

	public Type<T> parametized( Class<?>... arguments ) {
		Type<?>[] typeArgs = new Type<?>[arguments.length];
		for ( int i = 0; i < arguments.length; i++ ) {
			typeArgs[i] = raw( arguments[i] );
		}
		return parametized( typeArgs );
	}

	public Type<T> parametized( Type<?>... parameters ) {
		checkParameters( parameters );
		return new Type<T>( lowerBound, rawType, parameters );
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		toString( b );
		return b.toString();
	}

	void toString( StringBuilder b ) {
		if ( isLowerBound() ) {
			if ( rawType == Object.class ) {
				b.append( "?" );
				return;
			}
			b.append( "? extends " );
		}
		String canonicalName = rawType.getCanonicalName();
		b.append( rawType.isArray()
			? canonicalName.substring( 0, canonicalName.indexOf( '[' ) )
			: canonicalName );
		if ( isParameterized() ) {
			b.append( '<' );
			params[0].toString( b );
			for ( int i = 1; i < params.length; i++ ) {
				b.append( ',' );
				params[i].toString( b );
			}
			b.append( '>' );
		}
		if ( rawType.isArray() ) {
			b.append( canonicalName.substring( canonicalName.indexOf( '[' ) ) );
		}
	}

	private void checkParameters( Type<?>... parameters ) {
		if ( parameters.length == 0 ) {
			return; // its treated as raw-type
		}
		if ( isUnidimensionalArray() ) {
			elementRawType().checkParameters( parameters );
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
		for ( Type<?> s : type.supertypes() ) {
			if ( s.getRawType() == supertype ) {
				return (Type<? extends S>) s;
			}
		}
		throw new IllegalArgumentException( "`" + supertype + "` is not a supertype of: `" + type
				+ "`" );
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

	private <V> Map<String, Type<?>> actualTypeArguments( Type<V> type ) {
		Map<String, Type<?>> actualTypeArguments = new HashMap<String, Type<?>>();
		TypeVariable<Class<V>>[] typeParameters = type.rawType.getTypeParameters();
		for ( int i = 0; i < typeParameters.length; i++ ) {
			actualTypeArguments.put( typeParameters[i].getName(), isParameterized()
				? type.params[i]
				: WILDCARD ); // it would be correct to use the joint type of the bounds but since it is not possible to create a type with illegal parameters it is ok to just use Object since there is no way to model a joint type
		}
		return actualTypeArguments;
	}

	private void addSuperInterfaces( Set<Type<?>> res, Class<?> type,
			Map<String, Type<?>> actualTypeArguments ) {
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
		throw new UnsupportedOperationException( "The primitive " + primitive
				+ " cannot be wrapped yet!" );
	}

}