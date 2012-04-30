package de.jbee.silk;

public final class DeclaredType<T> {

	public static <T> DeclaredType<T> classtype( Class<T> type, T instance ) {

		return null;
	}

	public static <T> DeclaredType<T> type( Class<T> type ) {
		return new DeclaredType<T>( type );
	}

	private final Class<T> rawType;
	private final DeclaredType<?>[] typeArguments;

	DeclaredType( Class<T> rawType ) {
		super();
		this.rawType = rawType;
		this.typeArguments = new DeclaredType[0];
	}

	public DeclaredType<?>[] getTypeArguments() {
		return typeArguments;
	}

	public boolean isParameterized() {
		return typeArguments.length > 0;
	}

	public Class<T> getRawType() {
		return rawType;
	}

}
