package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Generator;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jbee.inject.Resource.resourceTypeOf;

/**
 * A test to demonstrate the difference between {@link se.jbee.inject.binder.Binder.TypedBinder#toGenerator(Generator)}
 * and {@link se.jbee.inject.binder.Binder.TypedBinder#toScopedGenerator(Generator)}.
 * <p>
 * Usually when binding to a {@link Generator} the API will assume that the user
 * wants the {@link Generator} to be used directly as the {@link Generator} is
 * the user facing API to generate instances. This means the backend facing
 * concept of the {@link se.jbee.inject.Supplier} which adds the {@link
 * se.jbee.inject.Scope} effects within the {@link Injector} is not involved in
 * instance creation.
 * <p>
 * If user just use the {@link Generator} interface because it was the more
 * convenient way to implement their instance generation but they still want the
 * usual instance creation process provided by a {@link se.jbee.inject.Supplier}
 * they can use the {@link se.jbee.inject.binder.Binder.TypedBinder#toScopedGenerator(Generator)
 * method instead.
 */
class TestBasicGeneratorBinds {

	private static final class TestBasicGeneratorBindsModule extends BinderModule
			implements Generator<String> {

		@Override
		protected void declare() {
			bind(String.class).toGenerator(this);
			AtomicInteger val = new AtomicInteger();
			bind(int.class).toScopedGenerator(dep -> val.incrementAndGet());
		}

		@Override
		public String generate(Dependency<? super String> dep) {
			return "hello world";
		}
	}

	private final Injector context = Bootstrap.injector(
			TestBasicGeneratorBindsModule.class);

	@Test
	void toGeneratorBypassesScopeEffects() {
		assertEquals("hello world", context.resolve(String.class));
		assertEquals("NonScopedGenerator", context.resolve(resourceTypeOf(
				String.class)).generator.getClass().getSimpleName());
	}

	@Test
	void toScopedGeneratorDoesNotBypassScopeEffects() {
		assertEquals(1, context.resolve(int.class));
		assertEquals(1, context.resolve(int.class),
				"second time is the real test - if this is scoped this is a application singleton so the value is still 1");
		// another way to verify it...
		assertEquals("LazyScopedGenerator", context.resolve(resourceTypeOf(
				int.class)).generator.getClass().getSimpleName());
	}
}
