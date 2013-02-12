package se.jbee.inject.config;

import java.util.EnumSet;

import se.jbee.inject.Packages;
import se.jbee.inject.Type;

public final class Globals {

	@SuppressWarnings("unchecked")
	public static <T extends Enum<T> & Feature<T>> Edition featureEdition( T... featured ) {
		return new FeatureEdition<T>( featured[0], EnumSet.of( featured[0], featured ) );
	}

	public static Edition packagesEdition( Packages included ) {
		return new PackagesEdition( included );
	}

	public static final Globals DEFAULT = new Globals( Edition.FULL, Options.STANDARD,
			Presets.NOTHING );

	public final Edition edition;
	public final Options options;
	public final Presets presets;

	private Globals( Edition edition, Options options, Presets presets ) {
		super();
		this.edition = edition;
		this.options = options;
		this.presets = presets;
	}

	public Globals edition( Edition edition ) {
		return new Globals( edition, options, presets );
	}

	public Globals edition( Packages included ) {
		return edition( packagesEdition( included ) );
	}

	@SuppressWarnings("unchecked")
	public <T extends Enum<T> & Feature<T>> Globals edition( T... featured ) {
		return edition( featureEdition( featured ) );
	}

	public Globals options( Options options ) {
		return new Globals( edition, options, presets );
	}

	public Globals presets( Presets presets ) {
		return new Globals( edition, options, presets );
	}

	private static class FeatureEdition<T extends Enum<T>>
			implements Edition {

		private final Feature<T> feature;
		private final EnumSet<T> featured;

		FeatureEdition( Feature<T> feature, EnumSet<T> featured ) {
			super();
			this.feature = feature;
			this.featured = featured;
		}

		@Override
		public boolean featured( Class<?> bundleOrModule ) {
			T f = feature.featureOf( bundleOrModule );
			return f == null || featured.contains( f );
		}
	}

	private static class PackagesEdition
			implements Edition {

		private final Packages included;

		PackagesEdition( Packages included ) {
			super();
			this.included = included;
		}

		@Override
		public boolean featured( Class<?> bundleOrModule ) {
			return included.contains( Type.raw( bundleOrModule ) );
		}

	}
}
