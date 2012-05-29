package de.jbee.inject;

public class Availability {

	public static final Availability EVERYWHERE = availability( Instance.ANY );

	public static Availability availability( Instance<?> target ) {
		return new Availability( target, "", -1 );
	}

	private final Instance<?> target;
	private final String path;
	private final int depth;

	private Availability( Instance<?> target, String path, int depth ) {
		super();
		this.target = target;
		this.path = path;
		this.depth = depth;
	}

	public Availability injectingInto( Instance<?> target ) {
		return new Availability( target, path, depth );
	}

	public boolean isApplicableFor( Dependency<?> dependency ) {

		return true;
	}

	@Override
	public String toString() {
		return "[" + path + "-" + depth + "-" + target + "]";
	}
}
