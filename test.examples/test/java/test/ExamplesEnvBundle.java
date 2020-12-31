package test;

import se.jbee.inject.Env;
import se.jbee.inject.Extends;
import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.binder.BundleFor;
import test.example1.Example1EnvBundle;
import test.example2.Example2EnvBundle;

@Extends(Env.class)
public class ExamplesEnvBundle extends BundleFor<Example> implements Bundle {

	@Override
	protected void bootstrap() {
		installDependentOn(Example.EXAMPLE_1, Example1EnvBundle.class);
		installDependentOn(Example.EXAMPLE_2, Example2EnvBundle.class);
	}

	@Override
	public void bootstrap(Bootstrapper bootstrap) {
		bootstrap.install(getClass(), Example.class);
	}
}
