package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.Installs;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.PublishesBy;
import se.jbee.inject.defaults.DefaultFeature;
import se.jbee.inject.defaults.DefaultFeatures;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.lang.Type.raw;

/**
 * Simply demonstration of how to add injection of {@link Optional} parameters.
 *
 * Dependencies that are available are injected wrapped in the {@link Optional},
 * dependencies that are not available are injected as {@link Optional#empty()}.
 */
class TestFeatureOptionalBinds {

	@Installs(features = DefaultFeature.class, by = DefaultFeatures.class)
	@DefaultFeatures(DefaultFeature.OPTIONAL)
	private static final class TestOptionalBindsModule extends BinderModule {

		@Override
		protected void declare() {
			withPublishedAccess().bind(int.class).to(5);
			bind(String.class).to("foo");
		}
	}

	private final Injector context = Bootstrap.injector(
			Bootstrap.DEFAULT_ENV.with(PublishesBy.class, PublishesBy.SUPER),
			TestOptionalBindsModule.class);

	@Test
	void optionalIsAvailableForExactType() {
		assertEquals(Optional.of(5), context.resolve(
				raw(Optional.class).parameterized(Integer.class)));
		assertEquals(Optional.of("foo"), context.resolve(
				raw(Optional.class).parameterized(String.class)));
	}

	@Test
	void optionalIsAvailableForSuperType() {
		assertEquals(Optional.of(5), context.resolve(
				raw(Optional.class).parameterized(Number.class)));

	}

	@Test
	void emptyOptionalReturnedOtherwise() {
		assertEquals(Optional.empty(),
				context.resolve(raw(Optional.class).parameterized(Float.class)));
	}

	@Test
	void optionalOfOptionalOfIsAvailableForExactType() {
		assertEquals(Optional.of(Optional.of(5)),
				context.resolve(raw(Optional.class).parameterized(
						raw(Optional.class).parameterized(Integer.class))));
	}

	@Test
	void emptyOptionalOfOptionalReturnedOtherwise() {
		assertEquals(Optional.of(Optional.empty()),
				context.resolve(raw(Optional.class).parameterized(
						raw(Optional.class).parameterized(Float.class))));
	}
}
