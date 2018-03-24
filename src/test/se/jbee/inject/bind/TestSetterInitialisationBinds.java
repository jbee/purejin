package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.jbee.inject.Dependency;
import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;

/**
 * A very basic test that shows how {@link Binder#init(Class)} can be used to
 * simulate setter injection.
 */
public class TestSetterInitialisationBinds {

	private static class Bean {

		String value;

		public void setFoo(String value) {
			this.value = value;
		}
	}

	private static class SetterInitialisationBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(String.class).to("foo");
			construct(Bean.class);
			init(Bean.class).with(String.class, Bean::setFoo);
		}

	}

	@Test
	public void setterInjectionCanBeSimulatedUsingInit() {
		Injector injector = Bootstrap.injector(SetterInitialisationBindsModule.class);
		Bean bean = injector.resolve( Dependency.dependency(Bean.class));

		assertEquals("foo", bean.value);
	}
}
