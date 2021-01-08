package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.binder.AbstractBinderModule;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestExampleLambdaBinds {

	static class TestExampleLambdaBindsBundle extends AbstractBinderModule {

		@Override
		public void bootstrap(Bootstrapper bootstrap) {
			bootstrap.install((bindings, env)
					-> bindings.declaredFrom(env,
					new TestExampleLambdaBindsModule()));
		}

	}

	static class TestExampleLambdaBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(int.class).to(1);
		}

	}

	@Test
	void thatLambdasCanBeUsedToDescribeModules() {
		Injector injector = Bootstrap.injector(TestExampleLambdaBindsModule.class);

		assertEquals(1, injector.resolve(int.class).intValue());
	}
}
