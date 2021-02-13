package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.*;
import static se.jbee.lang.Type.raw;

/**
 * This tests the behaviour of the {@link se.jbee.inject.config.ConstructsBy#OPTIMISTIC}
 * strategy.
 *
 * @see TestBasicHintsBinds for testing of the strategy involving {@link
 * se.jbee.inject.Hint}s.
 */
class TestFeatureOptimisticConstructorBinds {

	public static class IllegalSelfReference {

		public IllegalSelfReference(IllegalSelfReference endlessLoop, String another) {
			// such a constructor would most likely cause an endless loop
			// so it is ignored
			fail("should not be used even though it has more parameters");
		}

		public IllegalSelfReference(String justOne) {
			// this is ok so this is the constructor we want to pick
		}
	}

	public static class LegalSelfReference<T> {

		public LegalSelfReference(LegalSelfReference<String> other) {

		}
	}

	public static class PublicOverPrivate {

		final String origin;

		public PublicOverPrivate() {
			this("public");
		}

		private PublicOverPrivate(String value) {
			this.origin = value;
		}

		protected PublicOverPrivate(Integer a, Double b) {
			this("protected");
		}

		PublicOverPrivate(String value, Float f) {
			this("protected " + value);
		}
	}

	private static class TestFeatureOptimisticConstructorBindsModule extends
			BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("some value");
			construct(IllegalSelfReference.class);

			construct(LegalSelfReference.class);
			bind(raw(LegalSelfReference.class).parameterized(String.class))
					.to(new LegalSelfReference<>(null));

			construct(PublicOverPrivate.class);
		}
	}

	private final Injector context = Bootstrap.injector(
			TestFeatureOptimisticConstructorBindsModule.class);

	@Test
	void constructorsWithParametersOfSameTypeAreIgnored() {
		assertNotNull(context.resolve(IllegalSelfReference.class));
	}

	@Test
	void constructorsWithParametersOfSameTypeWithGenericsAreNotIgnored() {
		assertNotNull(context.resolve(
				raw(LegalSelfReference.class).parameterized(Integer.class)));
	}

	@Test
	void mostVisibleConstructorWinsOverMostParameterConstructor() {
		assertEquals("public", context.resolve(PublicOverPrivate.class).origin);
	}
}
