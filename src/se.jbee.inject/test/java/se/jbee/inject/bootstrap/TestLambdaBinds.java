package se.jbee.inject.bootstrap;

import org.junit.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.InitializedBinder;

import static org.junit.Assert.assertEquals;

public class TestLambdaBinds {

	static class LambdaBindsBundle extends InitializedBinder implements Bundle {

		@Override
		public void bootstrap(Bootstrapper bootstrap) {
			bootstrap.install((bindings, env) //
			-> bindings.declaredFrom(env, new LambdaBindsModule()));
		}

	}

	static class LambdaBindsModule extends BinderModule {

		@Override
		protected void declare() {
			bind(int.class).to(1);
		}

	}

	@Test
	public void thatLambdasCanBeUsedToDescribeModules() {
		Injector injector = Bootstrap.injector(LambdaBindsModule.class);

		assertEquals(1, injector.resolve(int.class).intValue());
	}
}
