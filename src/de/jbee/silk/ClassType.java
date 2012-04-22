package de.jbee.silk;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class ClassType<T>
		implements ParameterizedType {

	public static <T> ClassType<T> classtype( Class<T> type, T instance ) {

		return null;
	}

	public static <T> ClassType<T> type( Class<T> type ) {
		return new ClassType<T>( type, type );
	}

	private final Class<T> rawType;
	private final Type type;

	ClassType( Class<T> rawType, Type type ) {
		super();
		this.rawType = rawType;
		this.type = type;
	}

	@Override
	public Type[] getActualTypeArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type getOwnerType() {
		return null;
	}

	@Override
	public Class<T> getRawType() {
		return rawType;
	}

}
