package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.Lift;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static se.jbee.inject.Name.named;

/**
 * A very basic test that shows how {@link Binder#boot(Class)} can be used to
 * build explicit setter injection. That means each setter has to be bound.
 * <p>
 * In this particular example this is <b>not</b> adding the general feature of
 * injecting setters but wiring specific instances to be injected using the
 * available setter. This is a viable strategy to adapt to 3rd party classes
 * that assume setter injection but it should not be overused.
 * <p>
 * If setter injection is wanted in general a single {@link
 * Lift} or a custom {@link se.jbee.inject.bind.ValueBinder}
 * can be used similar to the example given in {@link TestExampleFieldInjectionBinds}.
 * <p>
 * Also note that {@link Binder#boot(Class)} is eager, it occurs at the end of
 * the bootstrapping of the {@link Injector} context while a {@link
 * Lift} as shown in {@link TestExampleFieldInjectionBinds} is
 * lazy as it first occurs when the target instance is created.
 *
 * @see TestExampleFieldInjectionBinds
 */
class TestExampleSetterInjectionBinds {

	public static class Bean {

		String foo;
		AnotherBean bar;

		public void setFoo(String value) {
			this.foo = value;
		}

		public void setBar(AnotherBean bar) {
			this.bar = bar;
		}
	}

	public static class AnotherBean {

	}

	private static class TestExampleSetterInjectionBindsModule extends BinderModule {

		@Override
		protected void declare() {
			construct(Bean.class);
			construct(AnotherBean.class);

			// link beans via setter
			boot(Bean.class).by(AnotherBean.class, Bean::setBar);

			// link a configuration via setter
			bind(named("bar"), String.class).to("foo");
			boot(Bean.class).by(named("bar"), String.class, Bean::setFoo);
		}
	}

	private final Injector context = Bootstrap.injector(
			TestExampleSetterInjectionBindsModule.class);

	@Test
	void setterInjectionCanBeAddedUsingLift() {
		Bean bean = context.resolve(Bean.class);
		AnotherBean anotherBean = context.resolve(AnotherBean.class);

		assertEquals("foo", bean.foo);
		assertSame(anotherBean, bean.bar);
	}
}
