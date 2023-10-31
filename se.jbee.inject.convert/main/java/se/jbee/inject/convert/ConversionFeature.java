package se.jbee.inject.convert;

import se.jbee.inject.Converter;
import se.jbee.inject.bind.Bootstrapper.DependentBootstrapper;
import se.jbee.inject.bind.Dependent;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.config.Connector;
import se.jbee.lang.Type;

import static se.jbee.inject.binder.spi.ConnectorBinder.CONVERSION_CONNECTOR;

public enum ConversionFeature implements Dependent<ConversionFeature> {

	/**
	 * Connects methods annotated with {@link Conversion.Aware} so that they are
	 * picked up as {@link Converter}s.
	 * <p>
	 * Annotated methods should have 1 parameter and a non void return type.
	 */
	AWARE,

	/**
	 * Registers a universal converter supplier which tries to supply a {@link
	 * Converter} for a {@link Type} pair that has not been bound explicitly.
	 * <p>
	 * These are either dynamically connected conversion methods (such as
	 * methods annotated with {@link Conversion}) or conversion chains provided
	 * by {@link ConvertTo} (as extracted from {@link Converts} annotations).
	 */
	UNIVERSAL;

	@Override
	public void bootstrap(
			DependentBootstrapper<ConversionFeature> bootstrapper) {
		bootstrapper.installDependentOn(AWARE, ConvertedAwareModule.class);
		bootstrapper.installDependentOn(UNIVERSAL,
				UniversalConverterModule.class);
	}

	private static class ConvertedAwareModule extends BinderModule {

		@Override
		protected void declare() {
			asDefault().bind(CONVERSION_CONNECTOR, Connector.class) //
					.to(UniversalConverterSupplier.class);
			convertIn(Conversion.Aware.class, Conversion.class);
		}
	}

	private static class UniversalConverterModule extends BinderModule {

		@Override
		protected void declare() {
			Type<?> any = Type.WILDCARD;
			@SuppressWarnings("rawtypes")
			Type<Converter> anyConverter = Type.raw(Converter.class)
					.parameterized(any, any);
			asDefault().bind(anyConverter) //
					.toSupplier(UniversalConverterSupplier::new);
		}
	}
}
