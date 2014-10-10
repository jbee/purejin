/*
 *  Copyright (c) 2012-2013, Jan Bernitt 
 *			
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * A description of an {@link Resource} together with its duration of life ({@link Expiry}).
 * 
 * @author Jan Bernitt (jan@jbee.se)
 * 
 * @param <T>
 *            The type of the resource
 */
public final class Emergence<T> {

	public static <T> Emergence<T> emergence( Resource<T> resource, Expiry expiry ) {
		return new Emergence<T>( resource, expiry );
	}

	private final Resource<T> resource;
	private final Expiry expiry;

	private Emergence( Resource<T> resource, Expiry expiry ) {
		super();
		this.resource = resource;
		this.expiry = expiry;
	}

	public Resource<T> getResource() {
		return resource;
	}

	public Expiry getExpiry() {
		return expiry;
	}

	@Override
	public String toString() {
		return expiry + " " + resource;
	}

}
