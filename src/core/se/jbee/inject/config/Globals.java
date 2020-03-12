/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

/**
 * A record containing all configuring data and strategies.
 *
 * The is immutable! All methods create new instances that reflect the change.
 *
 * @see Options
 * @see Choices
 * @see Edition
 * @see Annotations
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
@Deprecated
public final class Globals {

	/**
	 * The standard configuration with no special {@link Choices} or
	 * {@link Options} including all features.
	 */
	public static final Globals STANDARD = new Globals(Edition.FULL,
			Choices.NONE, Options.NONE, Annotations.DETECT);

	public final Edition edition;
	public final Choices choices;
	public final Options options;
	public final Annotations annotations;

	private Globals(Edition edition, Choices choices, Options options,
			Annotations annotations) {
		this.edition = edition;
		this.choices = choices;
		this.options = options;
		this.annotations = annotations;
	}

	public Globals with(Edition edition) {
		return new Globals(edition, choices, options, annotations);
	}

	public Globals with(Choices choices) {
		return new Globals(edition, choices, options, annotations);
	}

	public Globals with(Options options) {
		return new Globals(edition, choices, options, annotations);
	}

	public Globals with(Annotations annotations) {
		return new Globals(edition, choices, options, annotations);
	}

}
