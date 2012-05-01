package de.jbee.inject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;

/**
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Type<T> {

	public static Type<?> fieldType( Field field ) {
		return type( field.getType(), field.getGenericType() );
	}

	public static <T> Type<T> instanceType( Class<T> rawType, T instance ) {
		Class<?> type = instance.getClass();
		if ( type == rawType ) { // there will be no generic type arguments
			return new Type<T>( rawType );
		}
		java.lang.reflect.Type superclass = type.getGenericSuperclass();
		// TODO check if this is or has the raw type
		java.lang.reflect.Type[] interfaces = type.getGenericInterfaces();
		// TODO check if one of them is or has the raw type
		return null;
	}

	public static <T> Type<T> rawType( Class<T> type ) {
		return new Type<T>( type );
	}

	public static <T> Type<T> type( Class<T> rawType, java.lang.reflect.Type type ) {
		if ( type instanceof Class<?> ) {
			return rawType( rawType );
		}
		if ( type instanceof ParameterizedType ) {
			ParameterizedType pt = (ParameterizedType) type;
			if ( pt.getRawType() != rawType ) {
				throw new IllegalArgumentException( "The given raw type " + rawType
						+ " is not the raw type of the given type: " + type );
			}
			return new Type<T>( rawType, types( pt.getActualTypeArguments() ) );
		}
		throw notSupportedYet( type );
	}

	private static UnsupportedOperationException notSupportedYet( java.lang.reflect.Type type ) {
		return new UnsupportedOperationException( "Type has no support yet: " + type );
	}

	private static Type<?>[] types( java.lang.reflect.Type[] arguments ) {
		Type<?>[] args = new Type<?>[arguments.length];
		for ( int i = 0; i < arguments.length; i++ ) {
			args[i] = type( arguments[i] );
		}
		return args;
	}

	private static Type<?> type( java.lang.reflect.Type type ) {
		if ( type instanceof Class<?> ) {
			return rawType( (Class<?>) type );
		}
		if ( type instanceof ParameterizedType ) {
			return parameterizedtype( (ParameterizedType) type );
		}
		throw notSupportedYet( type );
	}

	private static <T> Type<?> parameterizedtype( ParameterizedType type ) {
		@SuppressWarnings ( "unchecked" )
		Class<T> rawType = (Class<T>) type.getRawType();
		return new Type<T>( rawType, types( type.getActualTypeArguments() ) );
	}

	private final Class<T> rawType;
	private final Type<?>[] args;

	/**
	 * Used to model lower bound wildcard types like <code>? extends Foo</code>
	 */
	private final boolean lowerBound;

	private Type( boolean lowerBound, Class<T> rawType, Type<?>[] arguments ) {
		this.rawType = rawType;
		this.args = arguments;
		this.lowerBound = lowerBound;
	}

	private Type( Class<T> rawType, Type<?>[] arguments ) {
		this( false, rawType, arguments );
	}

	private Type( Class<T> rawType ) {
		this( false, rawType, new Type<?>[0] );
	}

	public Type<? extends T> asLoweBound() {
		return new Type<T>( true, rawType, args );
	}

	public boolean equalTo( Type<?> other ) {
		if ( rawType != other.rawType ) {
			return false;
		}
		if ( args.length != other.args.length ) {
			return false;
		}
		for ( int i = 0; i < args.length; i++ ) {
			if ( !args[i].equalTo( other.args[i] ) ) {
				return false;
			}
		}
		return true;
	}

	public Type<?> getElementType() {
		return rawType.isArray()
			? new Type( rawType.getComponentType() ) //TODO add typeArguments?
			: this;
	}

	public Class<T> getRawType() {
		return rawType;
	}

	public Type<?>[] getArguments() {
		return args;
	}

	public boolean isAssignableFrom( Type<?> type ) {
		if ( !type.rawType.isAssignableFrom( rawType ) ) {
			return false;
		}
		//TODO
		return false;
	}

	public boolean isAssignableTo( Type<?> type ) {
		return type.isAssignableFrom( this );
	}

	/**
	 * @return true if this type describes the lower bound of the required types.
	 */
	public boolean isLowerBound() {
		return lowerBound;
	}

	public boolean isParameterized() {
		return args.length > 0;
	}

	public boolean isUnidimensionalArray() {
		return rawType.isArray() && !rawType.getComponentType().isArray();
	}

	public boolean morePreciseThan( Type<T> other ) {
		if ( !isParameterized() ) {
			return false; // it's equal
		}
		//TODO
		return true;
	}

	/**
	 * @return A {@link Type} having as its type arguments {@link #asLoweBound()}s.
	 */
	public Type<T> parametizedAsLowerBounds() {
		if ( !isParameterized() ) {
			return this;
		}
		Type<?>[] arguments = new Type<?>[args.length];
		for ( int i = 0; i < args.length; i++ ) {
			arguments[i] = args[i].asLoweBound();
		}
		return new Type<T>( lowerBound, rawType, arguments );
	}

	public Type<T> parametizedWith( Class<?>... arguments ) {
		Type<?>[] typeArgs = new Type<?>[arguments.length];
		for ( int i = 0; i < arguments.length; i++ ) {
			typeArgs[i] = rawType( arguments[i] );
		}
		return parametizedWith( typeArgs );
	}

	public Type<T> parametizedWith( Type<?>... arguments ) {
		validateTypeArguments( arguments );
		return new Type<T>( lowerBound, rawType, arguments );
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		toString( b );
		return b.toString();
	}

	void toString( StringBuilder b ) {
		if ( isLowerBound() ) {
			b.append( "? extends " );
		}
		b.append( rawType.getCanonicalName() );
		if ( isParameterized() ) {
			b.append( '<' );
			args[0].toString( b );
			for ( int i = 1; i < args.length; i++ ) {
				b.append( ',' );
				args[i].toString( b );
			}
			b.append( '>' );
		}
	}

	private void validateTypeArguments( Type<?>... arguments ) {
		TypeVariable<Class<T>>[] params = rawType.getTypeParameters();
		if ( params.length != arguments.length ) {
			//OPEN maybe we can allow to specify less than params - all not specified will be ?
			throw new IllegalArgumentException( "Invalid nuber of type arguments" );
		}
		// TODO check bounds fulfilled by arguments
	}
}
