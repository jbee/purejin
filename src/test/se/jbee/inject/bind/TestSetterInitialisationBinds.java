package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static se.jbee.inject.Name.named;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * A very basic test that shows how {@link Binder#init(Class)} can be used to
 * simulate setter injection.
 */
public class TestSetterInitialisationBinds {

	private static class Bean {

		String foo;
		AnotherBean bar;

		public void setFoo(String value) {
			this.foo = value;
		}

		public void setBar(AnotherBean bar) {
			this.bar = bar;

		}
	}

	private static class AnotherBean {

	}

	private static class SetterInitialisationBindsModule extends BinderModule {

		@Override
		protected void declare() {
			construct(Bean.class);
			construct(AnotherBean.class);

			// link beans via setter
			init(Bean.class).with(AnotherBean.class, Bean::setBar);

			// link a configuration via setter
			bind(named("bar"), String.class).to("foo");
			init(Bean.class).with(named("bar"), String.class, Bean::setFoo);
		}

	}

	@Test
	public void setterInjectionCanBeSimulatedUsingInit() {
		Injector injector = Bootstrap.injector(SetterInitialisationBindsModule.class);
		Bean bean = injector.resolve(Bean.class);
		AnotherBean anotherBean = injector.resolve(AnotherBean.class);

		assertEquals("foo", bean.foo);
		assertSame(anotherBean, bean.bar);
	}
}
