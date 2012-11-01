/*
 *  Copyright (c) 2012, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * A description of an {@link Instance} together with its duration of life ({@link Expiry}).
 * 
 * @author Jan Bernitt (jan.bernitt@gmx.de)
 * 
 * @param <T>
 *            The type of the instance
 */
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
