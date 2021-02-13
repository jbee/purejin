package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.binder.Binder.ScopedBinder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.lang.Type;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertSame;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.lang.Type.raw;

/**
 * A test that shows how to inject a specific instance into another type using
 * the {@link ScopedBinder#injectingInto(se.jbee.inject.Instance)} API.
 * <p>
 * In other words, how to create bindings that have an effect that is local to
 * the target instance identified by {@link Name} and {@link
 * Type}.
 * <p>
 * This sort of narrowing the validity of bindings is also called targeting. The
 * information build using the fluent API ends up in a {@link
 * se.jbee.inject.Resource}'s {@link se.jbee.inject.Target}. The {@link
 * se.jbee.inject.Target} describes the scenario in which the {@link
 * se.jbee.inject.Resource} is valid to use.
 * <p>
 * Besides the receiving target instance the {@link se.jbee.inject.Target} can
 * be narrowed to the parent and grandparent instances of the receiving instance
 * up the injection hierarchy as well as to the {@link se.jbee.inject.Packages}
 * in which the target instance is declared.
 *
 * Last but not least the {@link se.jbee.inject.Target} can limit access to only allow indirect access through an interface.
 *
 * @see TestFeatureIndirectAccessOnlyBinds
 * @see TestBasicPackageLocalBinds
 */
class TestBasicInjectingIntoBinds {

	/**
	 * We use different {@link B} constants to check if the different {@link A}s
	 * got their desired {@linkplain B}.
	 */
	static final B B_IN_A = new B();
	static final B B_EVERYWHERE_ELSE = new B();
	static final B B_IN_AWESOME_A = new B();
	static final B B_IN_SERIALIZABLE = new B();
	static final B B_IN_D = new B();

	private static class TestBasicInjectingIntoBindsModule
			extends BinderModule {

		@Override
		protected void declare() {
			// A
			construct(A.class);
			injectingInto(A.class).bind(B.class).to(B_IN_A);
			// special A
			construct("special", A.class);
			injectingInto("special", A.class).bind(B.class).to(B_EVERYWHERE_ELSE);
			// awesome A
			construct("awesome", A.class);
			injectingInto("awesome", A.class).bind(B.class).to(B_IN_AWESOME_A);
			// B
			bind(B.class).to(B_EVERYWHERE_ELSE);
			// C
			construct(C.class);
			injectingInto(Serializable.class).bind(B.class).to(B_IN_SERIALIZABLE);
			// D
			construct(D.class);
			injectingInto(D.class).bind(B.class).to(B_IN_D);
		}
	}

	public static class A {

		final B b;

		public A(B b) {
			this.b = b;
		}

	}

	public static class B {

		public B() {
			// make visible
		}
	}

	public static class C implements Serializable {

		final B b;

		public C(B b) {
			this.b = b;
		}
	}

	public static class D implements Serializable {

		final B b;

		public D(B b) {
			this.b = b;
		}
	}

	private final Injector context = Bootstrap.injector(
			TestBasicInjectingIntoBindsModule.class);

	@Test
	void bindWithTargetIsUsedWhenInjectingIntoIt() {
		assertSame(B_IN_A, context.resolve(A.class).b);
	}

	@Test
	void bindWithTargetIsNotUsedWhenNotInjectingIntoIt() {
		assertSame(B_EVERYWHERE_ELSE, context.resolve(B.class));
	}

	@Test
	void namedTargetIsUsedWhenInjectingIntoIt() {
		Instance<A> specialA = instance(named("special"), raw(A.class));
		B b = context.resolve(dependency(B.class).injectingInto(specialA));
		assertSame(B_EVERYWHERE_ELSE, b);
	}

	@Test
	void bindWithNamedTargetIsUsedWhenInjectingIntoIt() {
		assertSame(B_EVERYWHERE_ELSE, context.resolve("special", A.class).b);
		assertSame(B_IN_AWESOME_A, context.resolve("awesome", A.class).b);
	}

	@Test
	void thatBindWithInterfaceTargetIsUsedWhenInjectingIntoClassHavingThatInterface() {
		assertSame(B_IN_SERIALIZABLE, context.resolve(C.class).b);
	}

	@Test
	void bindWithExactClassTargetIsUsedWhenInjectingIntoClassHavingThatClassButAlsoAnInterfaceMatching() {
		assertSame(B_IN_D, context.resolve(D.class).b);
	}
}
