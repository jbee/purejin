package de.jbee.inject;

public final class Injection {

	private final Instance<?> dependency;
	private final Instance<?> target;

	Injection( Instance<?> dependency, Instance<?> target ) {
		super();
		this.dependency = dependency;
		this.target = target;
	}

	public Instance<?> getTarget() {
		return target;
	}

	public boolean equalTo( Injection other ) {
		return dependency.equalTo( other.dependency ) && target.equalTo( other.target );
	}

	@Override
	public String toString() {
		return "(" + dependency + "->" + target + ")";
	}
}
