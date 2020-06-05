package se.jbee.inject.bind;

import static org.junit.Assert.assertSame;
import static se.jbee.inject.Dependency.dependency;
import static se.jbee.inject.Instance.instance;
import static se.jbee.inject.Name.named;
import static se.jbee.inject.Type.raw;

import java.io.Serializable;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.Instance;
import se.jbee.inject.Name;
import se.jbee.inject.bind.Binder.ScopedBinder;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * A test that demonstrates how to inject a specific instance into another type
 * using the {@link ScopedBinder#injectingInto(se.jbee.inject.Instance)} method.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public class TestTargetedBinds {

	/**
	 * We use different {@link Bar} constants to check if the different
	 * {@link Foo}s got their desired {@linkplain Bar}.
	 */
	static final Bar BAR_IN_FOO = new Bar();
	static final Bar BAR_EVERYWHERE_ELSE = new Bar();
	static final Bar BAR_IN_AWESOME_FOO = new Bar();
	static final Bar BAR_IN_SERIALIZABLE = new Bar();
	static final Bar BAR_IN_QUX = new Bar();

	private static class TargetedBindsModule extends BinderModule {

		@Override
		protected void declare() {
			construct(Foo.class);
			injectingInto(Foo.class).bind(Bar.class).to(BAR_IN_FOO);
			bind(Bar.class).to(BAR_EVERYWHERE_ELSE);
			Name special = named("special");
			construct(special, Foo.class); // if we would use a type bind like to(Foo.class) it wouldn't work since we use a Foo that is not created as special Foo so it got the other Bar
			injectingInto(special, Foo.class).bind(Bar.class).to(
					BAR_EVERYWHERE_ELSE);
			Name awesome = named("awesome");
			construct(awesome, Foo.class);
			injectingInto(awesome, Foo.class).bind(Bar.class).to(
					BAR_IN_AWESOME_FOO);
			construct(Baz.class);
			TargetedBinder binder = injectingInto(Serializable.class);
			binder.bind(Bar.class).to(BAR_IN_SERIALIZABLE);
			construct(Qux.class);
			injectingInto(Qux.class).bind(Bar.class).to(BAR_IN_QUX);
		}
	}

	private static class Foo {

		final Bar bar;

		@SuppressWarnings("unused")
		Foo(Bar bar) {
			this.bar = bar;
		}

	}

	private static class Bar {

		Bar() {
			// make visible
		}
	}

	private static class Baz implements Serializable {

		final Bar bar;

		@SuppressWarnings("unused")
		Baz(Bar bar) {
			this.bar = bar;
		}
	}

	private static class Qux implements Serializable {

		final Bar bar;

		@SuppressWarnings("unused")
		Qux(Bar bar) {
			this.bar = bar;
		}
	}

	private final Injector injector = Bootstrap.injector(
			TargetedBindsModule.class);

	@Test
	public void thatBindWithTargetIsUsedWhenInjectingIntoIt() {
		assertSame(BAR_IN_FOO, injector.resolve(Foo.class).bar);
	}

	@Test
	public void thatBindWithTargetIsNotUsedWhenNotInjectingIntoIt() {
		assertSame(BAR_EVERYWHERE_ELSE, injector.resolve(Bar.class));
	}

	@Test
	public void thatNamedTargetIsUsedWhenInjectingIntoIt() {
		Instance<Foo> specialFoo = instance(named("special"), raw(Foo.class));
		Bar bar = injector.resolve(
				dependency(Bar.class).injectingInto(specialFoo));
		assertSame(BAR_EVERYWHERE_ELSE, bar);
	}

	@Test
	public void thatBindWithNamedTargetIsUsedWhenInjectingIntoIt() {
		assertSame(BAR_EVERYWHERE_ELSE,
				injector.resolve("special", Foo.class).bar);
		assertSame(BAR_IN_AWESOME_FOO,
				injector.resolve("awesome", Foo.class).bar);
	}

	@Test
	public void thatBindWithInterfaceTargetIsUsedWhenInjectingIntoClassHavingThatInterface() {
		assertSame(BAR_IN_SERIALIZABLE, injector.resolve(Baz.class).bar);
	}

	@Test
	public void thatBindWithExactClassTargetIsUsedWhenInjectingIntoClassHavingThatClassButAlsoAnInterfaceMatching() {
		assertSame(BAR_IN_QUX, injector.resolve(Qux.class).bar);
	}
}
