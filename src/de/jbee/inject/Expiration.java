package de.jbee.inject;

public final class Expiration {

	public static final Expiration NEVER = new Expiration( 0 );

	private final int level;

	public Expiration( int level ) {
		super();
		this.level = level;
	}

	public Expiration mostUnstable( Expiration other ) {
		return level > other.level
			? this
			: other;
	}
}
