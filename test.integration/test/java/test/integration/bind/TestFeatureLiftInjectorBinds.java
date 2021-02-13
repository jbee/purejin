package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.*;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.lang.Type;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static se.jbee.inject.Name.named;

/**
 * A test that demonstrates that the {@link Injector} can be decorated using a
 * {@link Lift} and that the decorated {@link Injector} is passed to
 * {@link Supplier}s when resolving {@link Dependency}s.
 */
class TestFeatureLiftInjectorBinds {

	public static class DecoratingInjector implements Injector,
			Lift<Injector> {

		private final Injector decorated;

		public DecoratingInjector(Injector decorated) {
			this.decorated = decorated;
		}

		@Override
		public <T> T resolve(Dependency<T> dependency)
				throws UnresolvableDependency {
			return decorated.resolve(dependency);
		}

		@Override
		public Injector lift(Injector target, Type<?> as,
				Injector context) {
			//OBS: context is the undecorated Injector (there might have been other decorations applied before)
			// use target instead!
			return new DecoratingInjector(target);
		}

	}

	public static class Bean {

		public Bean(Injector injector) {
			assertSame(DecoratingInjector.class, injector.getClass());
		}
	}

	static class TestFeatureLiftInjectorBindsModule extends BinderModule {

		@Override
		protected void declare() {
			lift().to(DecoratingInjector.class);
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
			TestFeatureLiftInjectorBindsModule.class);

	@Test
	void bootstrappingReturnsDecoratedInjector() {
		assertSame(DecoratingInjector.class, injector.getClass());
	}

	@Test
	void decoratedInjectorIsInjected() {
		assertNotNull(injector.resolve("constructed", Bean.class));
	}

	@Test
	void decoratedInjectorIsProvidedAsSupplierContext() {
		assertNotNull(injector.resolve("supplied", Bean.class));
	}
}
