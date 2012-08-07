package de.jbee.inject;

public final class Stability {

	public static final Stability STABLE = new Stability( 0 );

	private final int level;

	public Stability( int level ) {
		super();
		this.level = level;
	}

}
