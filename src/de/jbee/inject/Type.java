package de.jbee.inject;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A generic version of {@link Class} like {@link java.lang.reflect.Type} but without a complex
 * hierarchy. Instead all cases are represented as a general model. The key difference is that this
 * model just describes concrete types. So there is no representation for a {@link TypeVariable}.
 * 
 * There are some generic cases that are not supported right now because they haven't been needed.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Type<T>
		implements PreciserThan<Type<?>>, Parameter<T> {

	public static final Type<? extends Object> WILDCARD = raw( Object.class ).asLowerBound();

	public static Type<?> fieldType( Field field ) {
		return type( field.getType(), field.getGenericType() );
	}

	public static Type<?> returnType( Method method ) {
		return type( method.getReturnType(), method.getGenericReturnType() );
	}

	public static Type<?>[] parameterTypes( Constructor<?> constructor ) {
		return parameterTypes( constructor.getParameterTypes(),
				constructor.getGenericParameterTypes() );
	}

	public static Type<?>[] parameterTypes( Method method ) {
		return parameterTypes( method.getParameterTypes(), method.getGenericParameterTypes() );
	}

	private static Type<?>[] parameterTypes( Class<?>[] parameterTypes,
			java.lang.reflect.Type[] genericParameterTypes ) {
		Type<?>[] res = new Type<?>[parameterTypes.length];
		for ( int i = 0; i < res.length; i++ ) {
			res[i] = type( parameterTypes[i], genericParameterTypes[i] );
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

	public static <T> Type<T> type( Class<T> rawType, java.lang.reflect.Type type ) {
		return type( rawType, type, Collections.<String, Type<?>> emptyMap() );
	}

	private static <T> Type<T> type( Class<T> rawType, java.lang.reflect.Type type,
			Map<String, Type<?>> actualTypeArguments ) {
		if ( type == rawType ) {
			return raw( rawType );
		}
		if ( type instanceof ParameterizedType ) {
			ParameterizedType pt = (ParameterizedType) type;
			if ( pt.getRawType() != rawType ) {
				throw new IllegalArgumentException( "The given raw type " + rawType
						+ " is not the raw type of the given type: " + type );
			}
			return new Type<T>( rawType, types( pt.getActualTypeArguments(), actualTypeArguments ) );
		}
		throw notSupportedYet( type );
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
		throw notSupportedYet( type );
	}

	private static <T> Type<?> parameterizedType( ParameterizedType type,
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
		if ( !isParameterized() ) {
			return true; //raw type is ok - no parameters to check
		}

		if ( other.rawType == rawType ) { // both have the same rawType
			return allParametersAreAssignableTo( other );
		}

		// this raw type is extending the rawType passed - check if it is implemented direct or passed

		// there is another trivial case: type has ? extends object for all parameters - that means will allow all 
		return true;
	}

	public boolean allParametersAreAssignableTo( Type<?> other ) {
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

	/**
	 * @return true if this type describes the lower bound of the required types.
	 */
	public boolean isLowerBound() {
		return lowerBound;
	}

	public boolean isParameterized() {
		return params.length > 0;
	}

	public boolean hasTypeParameter() {
		return rawType.getTypeParameters().length > 0;
	}

	public boolean isUnidimensionalArray() {
		return rawType.isArray() && !rawType.getComponentType().isArray();
	}

	public boolean morePreciseThan( Type<?> other ) {
		if ( !rawType.isAssignableFrom( other.rawType ) ) {
			return true;
		}
		//FIXME before generics can be compared we need to make sure we compare the same rawtype- otherwise those generics might mean different things
		if ( ( hasTypeParameter() && !isParameterized() )
				|| ( isLowerBound() && !other.isLowerBound() ) ) {
			return false; // equal or other is a subtype of this
		}
		if ( ( other.hasTypeParameter() && !other.isParameterized() )
				|| ( !isLowerBound() && other.isLowerBound() ) ) {
			return true;
		}
		// FIXME the below code assumes that generics are at the same index 
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
	 * @return A {@link Type} having all its type arguments {@link #asLowerBound()}s. Use this to
	 *         model &lt;?&gt; wildcard generic.
	 */
	public Type<T> parametizedAsLowerBounds() { //TODO recursive version or one with a depth ?
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
		b.append( rawType.getCanonicalName() );
		if ( isParameterized() ) {
			b.append( '<' );
			params[0].toString( b );
			for ( int i = 1; i < params.length; i++ ) {
				b.append( ',' );
				params[i].toString( b );
			}
			b.append( '>' );
		}
	}

	private void checkParameters( Type<?>... parameters ) {
		if ( parameters.length == 0 ) {
			return; // its treated as raw-type
		}
		TypeVariable<Class<T>>[] params = rawType.getTypeParameters();
		if ( params.length != parameters.length ) {
			if ( isUnidimensionalArray() ) {
				elementRawType().checkParameters( parameters );
				return;
			}
			//OPEN maybe we can allow to specify less than params - all not specified will be ?
			throw new IllegalArgumentException( "Invalid nuber of type arguments" );
		}
		// TODO check bounds fulfilled by arguments
	}

	/**
	 * @return a list of all super-classes and super-interfaces of this type starting with the
	 *         direct super-class followed by the direct super-interfaces continuing by going up the
	 *         type hierarchy.
	 */
	@SuppressWarnings ( "unchecked" )
	public Type<? super T>[] supertypes() {
		Set<Type<? super T>> res = new LinkedHashSet<Type<? super T>>();
		Class<? super T> supertype = rawType;
		java.lang.reflect.Type genericSupertype = null;
		Type<? super T> type = this;
		Map<String, Type<?>> actualTypeArguments = actualTypeArguments( type );
		while ( supertype != null ) {
			if ( genericSupertype != null ) {
				type = type( supertype, genericSupertype, actualTypeArguments );
				res.add( type );
			}
			actualTypeArguments = actualTypeArguments( type );
			addSuperInterfaces( res, supertype, actualTypeArguments );
			genericSupertype = supertype.getGenericSuperclass();
			supertype = supertype.getSuperclass();
		}
		return (Type<? super T>[]) res.toArray( new Type<?>[res.size()] );
	}

	private <V> Map<String, Type<?>> actualTypeArguments( Type<V> type ) {
		Map<String, Type<?>> actualTypeArguments = new HashMap<String, Type<?>>();
		TypeVariable<Class<V>>[] typeParameters = type.rawType.getTypeParameters();
		for ( int i = 0; i < typeParameters.length; i++ ) {
			actualTypeArguments.put( typeParameters[i].getName(), isParameterized()
				? type.params[i]
				: WILDCARD ); //TODO use bounds in that case ?
		}
		return actualTypeArguments;
	}

	private void addSuperInterfaces( Set<Type<? super T>> res, Class<? super T> type,
			Map<String, Type<?>> actualTypeArguments ) {
		@SuppressWarnings ( "unchecked" )
		Class<? super T>[] interfaces = (Class<? super T>[]) type.getInterfaces();
		java.lang.reflect.Type[] genericInterfaces = type.getGenericInterfaces();
		for ( int i = 0; i < interfaces.length; i++ ) {
			Type<? super T> interfaceType = Type.type( interfaces[i], genericInterfaces[i],
					actualTypeArguments );
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

	@SuppressWarnings ( "unchecked" )
	public static <S> Type<? extends S> supertype( Class<S> supertype, Type<? extends S> type ) {
		for ( Type<?> s : type.supertypes() ) {
			if ( s.getRawType() == supertype ) {
				return (Type<? extends S>) s;
			}
		}
		throw notSupportedYet( supertype );
	}

}
