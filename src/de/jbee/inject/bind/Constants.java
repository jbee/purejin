package de.jbee.inject.bind;

import java.util.IdentityHashMap;

public final class Constants {

	public static final Constants NONE = new Constants(
			new IdentityHashMap<Class<? extends Enum<?>>, Enum<?>>() );

	private final IdentityHashMap<Class<? extends Enum<?>>, Enum<?>> values;

	private Constants( IdentityHashMap<Class<? extends Enum<?>>, Enum<?>> values ) {
		this.values = values;
	}

	@SuppressWarnings ( "unchecked" )
	public <C extends Enum<C> & Const> C value( Class<C> property ) {
		return (C) values.get( property );
	}

	@SuppressWarnings ( "unchecked" )
	public <C extends Enum<C> & Const> Constants def( C constant ) {
		if ( constant == null ) {
			return this;
		}
		IdentityHashMap<Class<? extends Enum<?>>, Enum<?>> clone = (IdentityHashMap<Class<? extends Enum<?>>, Enum<?>>) values.clone();
		clone.put( constant.getDeclaringClass(), constant );
		return new Constants( clone );
	}

	@Override
	public boolean equals( Object obj ) {
		return obj instanceof Constants && values.equals( ( (Constants) obj ).values );
	}

	@Override
	public int hashCode() {
		return values.hashCode();
	}

	@Override
	public String toString() {
		return values.toString();
	}
}
