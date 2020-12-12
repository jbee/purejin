package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.Binder;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static se.jbee.inject.Name.named;

/**
 * A very basic test that shows how {@link Binder#init(Class)} can be used to
 * build setter injection.
 * <p>
 * In this particular example this is not adding the general feature of
 * injecting setters but wiring specific instances to be injected using the
 * available setter. This is a viable strategy to adapt to 3rd party classes
 * that assume setter injection but it should not be overused. If setter
 * injection is wanted in general a single {@link se.jbee.inject.BuildUp} or a
 * custom {@link se.jbee.inject.bind.ValueBinder} can be used similar to the
 * example given in {@link TestExampleFieldInjectionBinds}.
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
			init(Bean.class).by(AnotherBean.class, Bean::setBar);

			// link a configuration via setter
			bind(named("bar"), String.class).to("foo");
			init(Bean.class).by(named("bar"), String.class, Bean::setFoo);
		}
	}

	private final Injector context = Bootstrap.injector(
			TestExampleSetterInjectionBindsModule.class);

	@Test
	void setterInjectionCanBeAddedUsingBuildUp() {
		Bean bean = context.resolve(Bean.class);
		AnotherBean anotherBean = context.resolve(AnotherBean.class);

		assertEquals("foo", bean.foo);
		assertSame(anotherBean, bean.bar);
	}
}
