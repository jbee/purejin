package se.jbee.inject.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.Serializable;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.UnresolvableDependency.IllegalAcccess;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * This tests demonstrates how the {@link Binder#withIndirectAccess()} method
 * can be used to restrict access to a implementation type to its interfaces.
 */
public class TestIndirectBinds {

	static interface Abstraction {

	}

	static class Implementation implements Abstraction {

	}

	static interface Abstraction2 {

	}

	static class Implementation2 implements Abstraction2 {

	}

	static class ValidReceiver {

		final Abstraction abs;

		public ValidReceiver(Abstraction abs) {
			this.abs = abs;
		}
	}

	static class InvalidReceiver {

		final Implementation impl;

		InvalidReceiver(Implementation impl) {
			this.impl = impl;
		}
	}

	static class InvalidNestedReceiver {

		final InvalidReceiver invalid;

		InvalidNestedReceiver(InvalidReceiver invalid) {
			this.invalid = invalid;
		}

	}

	static class ValidNestedReceiver {

		final ValidReceiver valid;

		ValidNestedReceiver(ValidReceiver valid) {
			this.valid = valid;
		}
	}

	static class TestIndirectBindsModule extends BinderModule {

		@Override
		protected void declare() {
			withIndirectAccess().bind(Serializable.class).to("42");
			withIndirectAccess().bind(Abstraction.class).to(
					Implementation.class);
			withIndirectAccess().autobind(
					Implementation2.class).toConstructor();
			construct(ValidReceiver.class);
			construct(InvalidReceiver.class);
			construct(ValidNestedReceiver.class);
			construct(InvalidNestedReceiver.class);
		}

	}

	private Injector injector = Bootstrap.injector(
			TestIndirectBindsModule.class);

	@Test
	public void indirectConstantResourcesCanBeResolvedViaInterface() {
		assertEquals("42", injector.resolve(Serializable.class));
	}

	@Test
	public void indirectReferencedResourcesCanBeResolvedViaInterface() {
		assertSame(Implementation.class,
				injector.resolve(Abstraction.class).getClass());
	}

	@Test
	public void indirectAutoboundResourcesCanBeResolvedViaInterface() {
		assertSame(Implementation2.class,
				injector.resolve(Abstraction2.class).getClass());
	}

	@Test
	public void indirectResourcesCanBeInjectedViaInterface() {
		assertSame(Implementation.class,
				injector.resolve(ValidReceiver.class).abs.getClass());
	}

	@Test
	public void indirectResourcesCanBeInjectedViaInterfaceInHierarchy() {
		assertSame(Implementation.class, injector.resolve(
				ValidNestedReceiver.class).valid.abs.getClass());
	}

	@Test(expected = IllegalAcccess.class)
	public void indirectReferencedResourcesCannotBeResolvedDirectly() {
		assertNotNull(injector.resolve(Implementation.class));
	}

	@Test(expected = IllegalAcccess.class)
	public void indirectAutoboundResourcesCannotBeResolvedDirectly() {
		assertNotNull(injector.resolve(Implementation2.class));
	}

	@Test(expected = IllegalAcccess.class)
	public void indirectResourcesCannotBeInjectedDirectly() {
		assertNotNull(injector.resolve(InvalidReceiver.class).impl);
	}

	@Test(expected = IllegalAcccess.class)
	public void indirectResourcesCannotBeInjectedDirectlyInHierarchy() {
		assertNotNull(
				injector.resolve(InvalidNestedReceiver.class).invalid.impl);
	}
}
