package se.jbee.inject.bind;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.jbee.inject.Injector;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.bootstrap.Bootstrapper;
import se.jbee.inject.bootstrap.Bundle;

public class TestLambdaBinds {

	static class LambdaBindsBundle extends InitializedBinder implements Bundle {

		@Override
		public void bootstrap(Bootstrapper bootstrap) {
			bootstrap.install(
					bindings -> bindings.declareFrom(new LambdaBindsModule()));
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
