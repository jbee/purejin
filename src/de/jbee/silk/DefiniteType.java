package de.jbee.silk;

import java.lang.reflect.Type;

public final class DefiniteType<T> {

	public static <T> DefiniteType<T> instanceType( Class<T> rawType, T instance ) {
		Class<?> type = instance.getClass();
		if ( type == rawType ) { // there will be no generic type arguments
			return new DefiniteType<T>( rawType );
		}
		Type superclass = type.getGenericSuperclass();
		// TODO check if this is or has the raw type
		Type[] interfaces = type.getGenericInterfaces();
		// TODO check if one of them is or has the raw type
		return null;
	}

	public static <T> DefiniteType<T> type( Class<T> type ) {
		return new DefiniteType<T>( type );
	}

	private final Class<T> rawType;
	private final DefiniteType<?>[] typeArguments;

	DefiniteType( Class<T> rawType ) {
		super();
		this.rawType = rawType;
		this.typeArguments = new DefiniteType[0];
	}

	public DefiniteType<?>[] getTypeArguments() {
		return typeArguments;
	}

	public boolean isParameterized() {
		return typeArguments.length > 0;
	}

	public Class<T> getRawType() {
		return rawType;
	}

	public boolean equalTo( DefiniteType<?> other ) {
		if ( rawType != other.rawType ) {
			return false;
		}
		if ( typeArguments.length != other.typeArguments.length ) {
			return false;
		}
		for ( int i = 0; i < typeArguments.length; i++ ) {
			if ( !typeArguments[i].equalTo( other.typeArguments[i] ) ) {
				return false;
			}
		}
		return true;
	}
}
