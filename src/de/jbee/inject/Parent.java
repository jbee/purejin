package de.jbee.inject;

public class Parent {

	private final Dependency<?> dependency;
	private final Instance<?> target;

	Parent( Dependency<?> dependency, Instance<?> target ) {
		super();
		this.dependency = dependency;
		this.target = target;
	}

	public Dependency<?> getDependency() {
		return dependency;
	}

	public Instance<?> getTarget() {
		return target;
	}
}
