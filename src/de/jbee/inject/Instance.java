package de.jbee.inject;

/**
 * Used to tell that we don#t want just one singleton at a time but multiple distinguished by the
 * {@link Discriminator} used.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 */
public final class Instance<T>
		implements Typed<T> {

	public static <T> Instance<T> defaultInstance( Type<T> type ) {
		return new Instance<T>( Discriminator.DEFAULT, type );
	}

	private final Discriminator discriminator;
	private final Type<T> type;

	private Instance( Discriminator discriminator, Type<T> type ) {
		super();
		this.discriminator = discriminator;
		this.type = type;
	}

	public boolean equalTo( Instance<T> other ) {

		return true;
	}

	public Instance<T> discriminableBy( Discriminator discriminator ) {
		return new Instance<T>( discriminator, type );
	}

	@Override
	public Type<T> getType() {
		return type;
	}
}
