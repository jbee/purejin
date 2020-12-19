/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.io.Serializable;

/**
 * Describes a "stack-frame" or "layer" within the injection process.
 * <p>
 * For example, when resolving instance A which causes resolving instance B the
 * resolution or injection of A becomes the first frame, the injection of B the
 * second and so forth.
 */
public final class Injection implements Serializable {

	public final Instance<?> dependency;
	public final Locator<?> target;
	public final ScopeLifeCycle lifeCycle;

	public Injection(Instance<?> dependency, Locator<?> target,
			ScopeLifeCycle lifeCycle) {
		this.dependency = dependency;
		this.target = target;
		this.lifeCycle = lifeCycle;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Injection && equalTo((Injection) obj);
	}

	@Override
	public int hashCode() {
		return dependency.hashCode() ^ target.hashCode();
	}

	public boolean equalTo(Injection other) {
		return this == other || dependency.equalTo(other.dependency)
			&& target.equalTo(other.target);
	}

	@Override
	public String toString() {
		return dependency + " from " + target;
	}

	public Injection ignoredScoping() {
		return new Injection(dependency, target, ScopeLifeCycle.ignore);
	}
}
