/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *	
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.config;

import java.util.EnumSet;

import se.jbee.inject.Packages;
import se.jbee.inject.Type;

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
public final class Globals {

	@SafeVarargs
	public static <T extends Enum<T> & Feature<T>> Edition featureEdition(
			T... featured) {
		return new FeatureEdition<>(featured[0],
				EnumSet.of(featured[0], featured));
	}

	public static Edition packagesEdition(Packages included) {
		return new PackagesEdition(included);
	}

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

	public Globals withEditionIncluding(Packages included) {
		return with(packagesEdition(included));
	}

	@SafeVarargs
	public final <T extends Enum<T> & Feature<T>> Globals withEditionInclduing(
			T... featured) {
		return with(featureEdition(featured));
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

	private static class FeatureEdition<T extends Enum<T>> implements Edition {

		private final Feature<T> feature;
		private final EnumSet<T> featured;

		FeatureEdition(Feature<T> feature, EnumSet<T> featured) {
			this.feature = feature;
			this.featured = featured;
		}

		@Override
		public boolean featured(Class<?> bundleOrModule) {
			T f = feature.featureOf(bundleOrModule);
			return f == null || featured.contains(f);
		}
	}

	private static class PackagesEdition implements Edition {

		private final Packages included;

		PackagesEdition(Packages included) {
			this.included = included;
		}

		@Override
		public boolean featured(Class<?> bundleOrModule) {
			return included.contains(Type.raw(bundleOrModule));
		}

	}
}
