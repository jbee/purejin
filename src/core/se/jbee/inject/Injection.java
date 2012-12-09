/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * Describes on "stack-frame" within the injection process.
 * 
 * @author Jan Bernitt (jan@jbee.se)
 */
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
