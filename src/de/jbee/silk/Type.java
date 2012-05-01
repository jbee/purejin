package de.jbee.silk;

import java.lang.reflect.TypeVariable;

/**
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Type<T> {

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

	private final Class<T> rawType;
	private final Type<?>[] typeArgs;
	/**
	 * Used to model lower bound wildcard types like <code>? extends Foo</code>
	 */
	private final boolean lowerBound;

	private Type( Class<T> rawType ) {
		this( false, rawType, new Type<?>[0] );
	}

	private Type( boolean lowerBound, Class<T> rawType, Type<?>[] typeArguments ) {
		this.rawType = rawType;
		this.typeArgs = typeArguments;
		this.lowerBound = lowerBound;
	}

	public Type<T> parametizedWith( Class<?>... typeArguments ) {
		Type<?>[] args = new Type<?>[typeArguments.length];
		for ( int i = 0; i < typeArguments.length; i++ ) {
			args[i] = rawType( typeArguments[i] );
		}
		return parametizedWith( args );
	}

	public Type<T> parametizedWith( Type<?>... typeArguments ) {
		validateTypeArguments( typeArguments );
		return new Type<T>( lowerBound, rawType, typeArguments );
	}

	public Type<? extends T> asLoweBound() {
		return new Type<T>( true, rawType, typeArgs );
	}

	private void validateTypeArguments( Type<?>... typeArguments ) {
		TypeVariable<Class<T>>[] typeParams = rawType.getTypeParameters();
		if ( typeParams.length != typeArguments.length ) {
			//OPEN maybe we can allow to specify less than params - all not specified will be ?
			throw new IllegalArgumentException( "Invalid nuber of type arguments" );
		}
	}

	public Type<?>[] getTypeArguments() {
		return typeArgs;
	}

	/**
	 * @return true if this type describes the lower bound of the required types.
	 */
	public boolean isLowerBound() {
		return lowerBound;
	}

	public boolean isParameterized() {
		return typeArgs.length > 0;
	}

	public Class<T> getRawType() {
		return rawType;
	}

	public boolean morePreciseThan( Type<T> other ) {
		if ( !isParameterized() ) {
			return false; // it's equal
		}
		//TODO
		return true;
	}

	public boolean isAssignableFrom( Type<?> type ) {
		if ( !type.rawType.isAssignableFrom( rawType ) ) {
			return false;
		}
		//TODO
		return false;
	}

	public boolean is1DimensionArray() {
		return rawType.isArray() && !rawType.getComponentType().isArray();
	}

	public boolean equalTo( Type<?> other ) {
		if ( rawType != other.rawType ) {
			return false;
		}
		if ( typeArgs.length != other.typeArgs.length ) {
			return false;
		}
		for ( int i = 0; i < typeArgs.length; i++ ) {
			if ( !typeArgs[i].equalTo( other.typeArgs[i] ) ) {
				return false;
			}
		}
		return true;
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
			typeArgs[0].toString( b );
			for ( int i = 1; i < typeArgs.length; i++ ) {
				b.append( ',' );
				typeArgs[i].toString( b );
			}
			b.append( '>' );
		}
	}

	public Type<?> getElementType() {
		return rawType.isArray()
			? new Type( rawType.getComponentType() ) //TODO add typeArguments?
			: this;
	}
}
