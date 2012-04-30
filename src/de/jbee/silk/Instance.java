package de.jbee.silk;

/**
 * Used to tell that we don#t want just one singleton at a time but multiple distinguished by the
 * {@link Discriminator} used.
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 */
public final class Instance<T> {

	public static <T> Instance<T> defaultInstance( DeclaredType<T> type ) {
		return new Instance<T>( Discriminator.DEFAULT, type );
	}

	private final Discriminator discriminator;
	private final DeclaredType<T> type;

	Instance( Discriminator discriminator, DeclaredType<T> type ) {
		super();
		this.discriminator = discriminator;
		this.type = type;
	}

}
