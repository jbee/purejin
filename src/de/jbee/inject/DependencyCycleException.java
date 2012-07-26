package de.jbee.inject;

public class DependencyCycleException
		extends RuntimeException {

	private final Dependency<?> dependency;
	private final Instance<?> cycleTarget;

	public DependencyCycleException( Dependency<?> dependency, Instance<?> cycleTarget ) {
		super();
		this.dependency = dependency;
		this.cycleTarget = cycleTarget;
	}

	@Override
	public String toString() {
		return cycleTarget + " into " + dependency;
	}
}
