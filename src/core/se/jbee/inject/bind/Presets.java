package se.jbee.inject.bind;

import java.util.IdentityHashMap;

import se.jbee.inject.Type;

public final class Presets {

	public static final Presets NOTHING = new Presets( new IdentityHashMap<String, Object>( 0 ) );

	private final IdentityHashMap<String, Object> values;

	private Presets( IdentityHashMap<String, Object> values ) {
		super();
		this.values = values;
	}

	public <T> Presets preset( Class<T> type, T value ) {
		return preset( Type.raw( type ), value );
	}

	public <T> Presets preset( Type<T> type, T value ) {
		final String key = key( type );
		if ( value == null && !values.containsKey( key ) ) {
			return this;
		}
		@SuppressWarnings ( "unchecked" )
		IdentityHashMap<String, Object> clone = (IdentityHashMap<String, Object>) values.clone();
		if ( value == null ) {
			clone.remove( key );
		} else {
			clone.put( key, value );
		}
		return new Presets( clone );
	}

	@SuppressWarnings ( "unchecked" )
	public <T> T value( Type<T> type ) {
		return (T) values.get( key( type ) );
	}

	private static <T> String key( Type<T> type ) {
		return type.toString().intern();
	}

	@Override
	public String toString() {
		return values.toString();
	}
}
