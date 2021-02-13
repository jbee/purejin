package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.Supplier;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.config.PublishesBy;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static se.jbee.lang.Type.raw;

/**
 * The tests demonstrates the meaning of a {@link Binder#withPublishedAccess()}
 * call. That will create multiple binds, one each for the type and all its
 * super-classes and -interfaces. All of them are bound to the same to-clause,
 * hence share the same {@link Supplier} (in the end all to-clauses become
 * one).
 * <p>
 * This is special since we always ask for an explicitly bound type when
 * resolving a {@link Dependency}. That means just using {@link
 * Binder#bind(Class)} just makes the {@link Class} resolvable passed to the
 * bind method. Usually this is what we want and need to gain a predictable
 * setup. But in some cases an instance should serve as many different
 * interfaces all implemented by it (e.g. a class implementing a couple of
 * single service interfaces).
 */
class TestBasicSuperbindBinds {

	static class TestBasicSuperbindBindsModule extends BinderModule {

		@Override
		protected void declare() {
			withPublishedAccess().bind(Integer.class).to(42);
			withPublishedAccess()
					.bind(raw(List.class).parameterized(String.class)).to(emptyList());
			withPublishedAccess(((api, impl) -> api == CharSequence.class))
					.bind(String.class).to("answer");
		}
	}

	private final Injector context = Bootstrap.injector(
			Bootstrap.DEFAULT_ENV.with(PublishesBy.class, PublishesBy.SUPER),
			TestBasicSuperbindBindsModule.class);

	@Test
	void superbindTypeItselfIsBound() {
		assertEquals(42, context.resolve(Integer.class).intValue());
	}

	@Test
	void directSuperclassOfSuperbindTypeIsBound() {
		assertEquals(42, context.resolve(Number.class).intValue());
	}

	@Test
	void superInterfaceOfSuperbindTypeIsBound() {
		assertEquals(42, context.resolve(Serializable.class));
	}

	@Test
	void parametrizedSuperInterfaceOfSuperbindTypeIsBound() {
		assertEquals(42, context.resolve(
				raw(Comparable.class).parameterized(Integer.class)));
	}

	@Test
	void adHocStrategyIsApplied() {
		assertEquals("answer", context.resolve(CharSequence.class));
		assertThrows(UnresolvableDependency.ResourceResolutionFailed.class,
				() -> context.resolve(String.class));
	}
}
