/*
 *  Copyright (c) 2012-2017, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;


/**
 * Describes a "stack-frame" within the injection process.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class Injection {

	public final Instance<?> dependency;
	public final Resource<?> target;
	public final Expiry expiry;

	public Injection(Instance<?> dependency, Resource<?> target, Expiry expiry) {
		this.dependency = dependency;
		this.target = target;
		this.expiry = expiry;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Injection && equalTo((Injection) obj);
	}

	@Override
	public int hashCode() {
		return dependency.hashCode() ^ target.hashCode();
	}

	public boolean equalTo( Injection other ) {
		return this == other || dependency.equalTo( other.dependency )
				&& target.equalTo( other.target );
	}

	@Override
	public String toString() {
		return dependency + " for " + target+" ?";
	}

	public Injection ignoredExpiry() {
		return new Injection( dependency, target, Expiry.IGNORE );
	}
}
