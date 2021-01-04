package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.UnresolvableDependency.IllegalAccess;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.PublishesBy;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This tests demonstrates how the {@link Binder#withIndirectAccess()} method
 * can be used to restrict access to a implementation type to its interfaces.
 */
class TestFeatureIndirectAccessOnlyBinds {

	interface Abstraction {

	}

	public static class Implementation implements Abstraction {

	}

	interface Abstraction2 {

	}

	public static class Implementation2 implements Abstraction2 {

	}

	public static class ValidReceiver {

		final Abstraction abs;

		public ValidReceiver(Abstraction abs) {
			this.abs = abs;
		}
	}

	public static class InvalidReceiver {

		final Implementation impl;

		public InvalidReceiver(Implementation impl) {
			this.impl = impl;
		}
	}

	public static class InvalidNestedReceiver {

		final InvalidReceiver invalid;

		public InvalidNestedReceiver(InvalidReceiver invalid) {
			this.invalid = invalid;
		}

	}

	public static class ValidNestedReceiver {

		final ValidReceiver valid;

		public ValidNestedReceiver(ValidReceiver valid) {
			this.valid = valid;
		}
	}

	static class TestFeatureIndirectAccessOnlyBindsModule extends BinderModule {

		@Override
		protected void declare() {
			withIndirectAccess().bind(Serializable.class).to("42");
			withIndirectAccess().bind(Abstraction.class).to(
					Implementation.class);
			withIndirectAccess().withPublishedAccess().bind(
					Implementation2.class).toConstructor();
			construct(ValidReceiver.class);
			construct(InvalidReceiver.class);
			construct(ValidNestedReceiver.class);
			construct(InvalidNestedReceiver.class);
		}

	}

	private final Injector injector = Bootstrap.injector(
			Bootstrap.DEFAULT_ENV.with(PublishesBy.class, PublishesBy.SUPER),
			TestFeatureIndirectAccessOnlyBindsModule.class);

	@Test
	void indirectConstantResourcesCanBeResolvedViaInterface() {
		assertEquals("42", injector.resolve(Serializable.class));
	}

	@Test
	void indirectReferencedResourcesCanBeResolvedViaInterface() {
		assertSame(Implementation.class,
				injector.resolve(Abstraction.class).getClass());
	}

	@Test
	void indirectAutobindResourcesCanBeResolvedViaInterface() {
		assertSame(Implementation2.class,
				injector.resolve(Abstraction2.class).getClass());
	}

	@Test
	void indirectResourcesCanBeInjectedViaInterface() {
		assertSame(Implementation.class,
				injector.resolve(ValidReceiver.class).abs.getClass());
	}

	@Test
	void indirectResourcesCanBeInjectedViaInterfaceInHierarchy() {
		assertSame(Implementation.class, injector.resolve(
				ValidNestedReceiver.class).valid.abs.getClass());
	}

	@Test
	void indirectReferencedResourcesCannotBeResolvedDirectly() {
		assertThrows(IllegalAccess.class,
				() -> injector.resolve(Implementation.class));
	}

	@Test
	void indirectAutobindResourcesCannotBeResolvedDirectly() {
		assertThrows(UnresolvableDependency.IllegalAccess.class,
				() -> injector.resolve(Implementation2.class));
	}

	@Test
	void indirectResourcesCannotBeInjectedDirectly() {
		assertThrows(UnresolvableDependency.IllegalAccess.class,
				() -> injector.resolve(InvalidReceiver.class));
	}

	@Test
	void indirectResourcesCannotBeInjectedDirectlyInHierarchy() {
		assertThrows(UnresolvableDependency.IllegalAccess.class,
				() -> injector.resolve(InvalidNestedReceiver.class));
	}
}
