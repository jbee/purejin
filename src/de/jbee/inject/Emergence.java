/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package de.jbee.inject;

public final class Emergence<T> {

	public static <T> Emergence<T> emergence( Instance<T> instance, Expiry expiry ) {
		return new Emergence<T>( instance, expiry );
	}

	private final Instance<T> instance;
	private final Expiry expiry;

	private Emergence( Instance<T> instance, Expiry expiry ) {
		super();
		this.instance = instance;
		this.expiry = expiry;
	}

	public Instance<T> getInstance() {
		return instance;
	}

	public Expiry getExpiry() {
		return expiry;
	}

	@Override
	public String toString() {
		return expiry + " " + instance;
	}

}
