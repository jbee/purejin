package de.jbee.inject;

public class Availability
		implements PreciserThan<Availability> {

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
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String toString() {
		if ( target.isAny() && isGlobal() && depth < 0 ) {
			return "everywhere";
		}
		return "[" + path + "-" + depth + "-" + target + "]";
	}

	public Availability within( String path ) {
		return new Availability( target, path, depth );
	}

	public boolean isTrageted() {
		return !target.isAny();
	}

	public boolean isLocal() {
		return !isGlobal();
	}

	private boolean isGlobal() {
		return path.isEmpty();
	}

	@Override
	public boolean morePreciseThan( Availability other ) {
		if ( isLocal() && other.isGlobal() ) {
			return true;
		}
		if ( other.isLocal() && isGlobal() ) {
			return false;
		}
		//FIXME what about the case that path is a subpackage of other package or other way around ? ---> if they are excluding each other both are equal
		if ( isLocal() && other.isLocal() && depth != other.depth ) {
			return depth < other.depth;
		}
		return target.morePreciseThan( other.target );
	}
}
