package de.jbee.inject;

public final class Injection {

	private final Instance<?> dependency;
	private final Emergence<?> target;

	Injection( Instance<?> dependency, Emergence<?> target ) {
		super();
		this.dependency = dependency;
		this.target = target;
	}

	public Emergence<?> getTarget() {
		return target;
	}

	public boolean equalTo( Injection other ) {
		return this == other || dependency.equalTo( other.dependency )
				&& target.getInstance().equalTo( other.target.getInstance() );
	}

	@Override
	public String toString() {
		return "(" + dependency + "->" + target + ")";
	}
}
