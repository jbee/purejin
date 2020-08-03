package se.jbee.inject.bootstrap;

import org.junit.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static se.jbee.inject.Name.named;

/**
 * A test that demonstrates that the {@link Injector} can be decorated using a
 * {@link Initialiser} and that the decorated {@link Injector} is passed to
 * {@link Supplier}s when resolving {@link Dependency}s.
 */
public class TestInjectorDecorationBinds {

	static class DecoratingInjector implements Injector, Initialiser<Injector> {

		private final Injector decorated;

		DecoratingInjector(Injector decorated) {
			this.decorated = decorated;
		}

		@Override
		public <T> T resolve(Dependency<T> dependency)
				throws UnresolvableDependency {
			return decorated.resolve(dependency);
		}

		@Override
		public Injector init(Injector target, Injector context) {
			//OBS: context is the undecorated Injector (there might have been other decorations applied before)
			// use target instead!
			return new DecoratingInjector(target);
		}

	}

	static class Bean {

		Bean(Injector injector) {
			assertSame(DecoratingInjector.class, injector.getClass());
		}
	}

	static class TestInjectorDecorationBindsModule extends BinderModule {

		@Override
		protected void declare() {
			initbind().to(DecoratingInjector.class);
			bind(named("supplied"), Bean.class).toSupplier(this::supply);
			bind(named("constructed"), Bean.class).toConstructor();
		}

		Bean supply(@SuppressWarnings("unused") Dependency<? super Bean> dep,
				Injector context) throws UnresolvableDependency {
			assertSame(DecoratingInjector.class, context.getClass());
			return new Bean(context);
		}
	}

	private static Injector injector = Bootstrap.injector(
			TestInjectorDecorationBindsModule.class);

	@Test
	public void bootstrappingReturnsDecoratedInjector() {
		assertSame(DecoratingInjector.class, injector.getClass());
	}

	@Test
	public void decoratedInjectorIsInjected() {
		assertNotNull(injector.resolve("constructed", Bean.class));
	}

	@Test
	public void decoratedInjectorIsProvidedAsSupplierContext() {
		assertNotNull(injector.resolve("supplied", Bean.class));
	}
}
