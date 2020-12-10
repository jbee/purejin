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
 * simulate setter injection.
 */
class TestSetterInitialisationBinds {

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

	private static class SetterInitialisationBindsModule extends BinderModule {

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

	@Test
	void setterInjectionCanBeSimulatedUsingInit() {
		Injector injector = Bootstrap.injector(
				SetterInitialisationBindsModule.class);
		Bean bean = injector.resolve(Bean.class);
		AnotherBean anotherBean = injector.resolve(AnotherBean.class);

		assertEquals("foo", bean.foo);
		assertSame(anotherBean, bean.bar);
	}
}
