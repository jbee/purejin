package de.jbee.inject;

public class DependencyCycleException
		extends RuntimeException {

	private final Dependency<?> dependency;
	private final Instance<?> cycleTarget;

	public DependencyCycleException( Dependency<?> dependency, Instance<?> cycleTarget ) {
		super( cycleTarget + " into " + dependency );
		this.dependency = dependency;
		this.cycleTarget = cycleTarget;
	}

	@Override
	public String toString() {
		return getMessage();
	}
}
