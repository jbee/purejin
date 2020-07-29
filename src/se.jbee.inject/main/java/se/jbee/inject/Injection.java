/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

import java.io.Serializable;

/**
 * Describes a "stack-frame" or "layer" within the injection process.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Injection implements Serializable {

	public final Instance<?> dependency;
	public final Locator<?> target;
	public final ScopePermanence scoping;

	public Injection(Instance<?> dependency, Locator<?> target,
			ScopePermanence scoping) {
		this.dependency = dependency;
		this.target = target;
		this.scoping = scoping;
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
		return dependency + " for " + target + " ?";
	}

	public Injection ignoredScoping() {
		return new Injection(dependency, target, ScopePermanence.ignore);
	}
}
