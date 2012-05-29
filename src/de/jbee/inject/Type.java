package de.jbee.inject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;

/**
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 */
public final class Type<T>
		implements Comparable<Type<?>> {

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

	public static <T> Type<T> supertype( Type<? extends T> actualType, Class<T> rawType,
			java.lang.reflect.Type type ) {

		return null;
	}

	private static UnsupportedOperationException notSupportedYet( java.lang.reflect.Type type ) {
		return new UnsupportedOperationException( "Type has no support yet: " + type );
	}

	private static Type<?>[] types( java.lang.reflect.Type[] parameters ) {
		Type<?>[] args = new Type<?>[parameters.length];
		for ( int i = 0; i < parameters.length; i++ ) {
			args[i] = type( parameters[i] );
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
		if ( type instanceof TypeVariable<?> ) {
			// TODO
		}
		throw notSupportedYet( type );
	}

	private static <T> Type<?> parameterizedtype( ParameterizedType type ) {
		@SuppressWarnings ( "unchecked" )
		Class<T> rawType = (Class<T>) type.getRawType();
		return new Type<T>( rawType, types( type.getActualTypeArguments() ) );
	}

	private final Class<T> rawType;
	private final Type<?>[] params;

	/**
	 * Used to model lower bound wildcard types like <code>? extends Foo</code>
	 */
	private final boolean lowerBound;

	private Type( boolean lowerBound, Class<T> rawType, Type<?>[] parameters ) {
		this.rawType = rawType;
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
		return new Type<T>( true, rawType, params );
	}

	public Type<? extends T> exact() {
		return new Type<T>( false, rawType, params );
	}

	public boolean equalTo( Type<?> other ) {
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

	public Type<?> getElementType() {
		Type<?> elemRawType = getElementRawType();
		return elemRawType == this
			? this
			: elemRawType.parametized( params );
	}

	private Type<?> getElementRawType() {
		return rawType.isArray()
			? new Type( rawType.getComponentType() )
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
		return other.isLowerBound() && isAssignableTo( other.exact() );
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
		if ( !isParameterized() || allArgumentsAreLowerBounds() ) {
			return this;
		}
		Type<?>[] parameters = new Type<?>[params.length];
		for ( int i = 0; i < params.length; i++ ) {
			parameters[i] = params[i].asLowerBound();
		}
		return new Type<T>( lowerBound, rawType, parameters );
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
			typeArgs[i] = rawType( arguments[i] );
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
				getElementRawType().checkParameters( parameters );
				return;
			}
			//OPEN maybe we can allow to specify less than params - all not specified will be ?
			throw new IllegalArgumentException( "Invalid nuber of type arguments" );
		}
		// TODO check bounds fulfilled by arguments
	}

	public Type<?> asSupertype( Class<? super T> supertype ) {
		if ( supertype == rawType ) {
			return this;
		}
		Class<? super T> superclass = rawType.getSuperclass();
		java.lang.reflect.Type genericSuperclass = rawType.getGenericSuperclass();
		while ( superclass != null && superclass != supertype ) {
			genericSuperclass = superclass.getGenericSuperclass();
			superclass = superclass.getSuperclass();
		}
		if ( superclass == supertype ) {
			return supertype( this, superclass, genericSuperclass );
		}
		@SuppressWarnings ( "unchecked" )
		Class<? super T>[] interfaces = (Class<? super T>[]) rawType.getInterfaces();
		java.lang.reflect.Type[] genericInterfaces = rawType.getGenericInterfaces();
		for ( int i = 0; i < interfaces.length; i++ ) {
			if ( interfaces[i] == supertype ) {
				return supertype( this, interfaces[i], genericInterfaces[i] );
			}
		}
		throw new RuntimeException( "Couldn't find supertype " + supertype + " for type: " + this );
	}

	@Override
	public int compareTo( Type<?> other ) {
		if ( morePreciseThan( other ) ) {
			return 1;
		}
		if ( other.morePreciseThan( this ) ) {
			return -1;
		}
		return rawType == other.rawType
			? 0
			: rawType.getCanonicalName().compareTo( other.rawType.getCanonicalName() );
	}
}
