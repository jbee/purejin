package de.jbee.inject;

public class Availability {

	public static final Availability EVERYWHERE = new Availability( Instance.ANY );

	private final Instance<?> target;

	Availability( Instance<?> target ) {
		super();
		this.target = target;
	}

	public Availability targetedAt( Instance<?> target ) {
		return new Availability( target );
	}

	public boolean isApplicableFor( Dependency<?> dependency ) {

		return true;
	}
}
