/*
 *  Copyright (c) 2012-2017, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject;

/**
 * The data part of a {@link Injectron}.
 *
 * @param <T> type of instances yielded by the {@link Injectron} described by this info
 */
public final class InjectronInfo<T> {

	/**
	 * The {@link Resource} represented by the {@link Injectron} of this info.
	 */
	public final Resource<T> resource;

	/**
	 * The {@link Source} that {@link Injection} had been created from (e.g. did
	 * define the bind).
	 */
	public final Source source;

	/**
	 * The frequency in which this injectron's {@link Resource} expires.
	 */
	public final Expiry expiry;

	/**
	 * the serial ID of the {@link Injectron} being injected.
	 */
	public final int serialID;

	/**
	 * the total amount of {@link Injectron}s in the same ({@link Injector})
	 * container.
	 */
	public final int count;

	public InjectronInfo(Resource<T> resource, Source source, Expiry expiry, int serialID, int count) {
		this.resource = resource;
		this.source = source;
		this.expiry = expiry;
		this.serialID = serialID;
		this.count = count;
	}

	@Override
	public String toString() {
		return serialID + " " + resource + " " + source + " " + expiry;
	}
}
