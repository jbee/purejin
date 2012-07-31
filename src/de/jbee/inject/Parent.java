package de.jbee.inject;

public class Parent {

	private final Instance<?> dependency;
	private final Instance<?> target;

	Parent( Instance<?> dependency, Instance<?> target ) {
		super();
		this.dependency = dependency;
		this.target = target;
	}

	public Instance<?> getDependency() {
		return dependency;
	}

	public Instance<?> getTarget() {
		return target;
	}

	@Override
	public String toString() {
		return "{" + dependency + " -> " + target + "}";
	}
}
