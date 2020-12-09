package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.CoreFeature;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.lang.Type.raw;

/**
 * Simply demonstration of how to add injection of {@link Optional} parameters.
 *
 * Dependencies that are available are injected wrapped in the {@link Optional},
 * dependencies that are not available are injected as {@link Optional#empty()}.
 */
class TestOptionalBinds {

	static final class TestOptionalBindsModule extends BinderModule {

		@Override
		protected void declare() {
			autobind(int.class).to(5);
			bind(String.class).to("foo");
		}

	}

	static final class TestOptionalBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(TestOptionalBindsModule.class);
			install(CoreFeature.OPTIONAL);
		}

	}

	private final Injector injector = Bootstrap.injector(
			TestOptionalBindsBundle.class);

	@Test
	public void optionalIsAvailableForExactType() {
		assertEquals(Optional.of(5), injector.resolve(
				raw(Optional.class).parametized(Integer.class)));
		assertEquals(Optional.of("foo"), injector.resolve(
				raw(Optional.class).parametized(String.class)));
	}

	@Test
	public void optionalIsAvailableForSuperType() {
		assertEquals(Optional.of(5), injector.resolve(
				raw(Optional.class).parametized(Number.class)));

	}

	@Test
	public void emptyOptionalReturnedOtherwise() {
		assertEquals(Optional.empty(),
				injector.resolve(raw(Optional.class).parametized(Float.class)));
	}

	@Test
	public void optionalOfOptionalOfIsAvailableForExactType() {
		assertEquals(Optional.of(Optional.of(5)),
				injector.resolve(raw(Optional.class).parametized(
						raw(Optional.class).parametized(Integer.class))));
	}

	@Test
	public void emptyOptionalOfOptionalReturnedOtherwise() {
		assertEquals(Optional.of(Optional.empty()),
				injector.resolve(raw(Optional.class).parametized(
						raw(Optional.class).parametized(Float.class))));
	}
}
