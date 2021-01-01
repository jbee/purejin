package test;

import se.jbee.inject.bind.Bootstrapper;
import se.jbee.inject.bind.Bundle;
import se.jbee.inject.binder.BundleFor;
import se.jbee.inject.defaults.DefaultFeature;
import test.example1.Example1RootBundle;
import test.example2.Example2Bundle;

public class ExamplesBundle extends BundleFor<Example> implements Bundle {
	@Override
	protected void bootstrap() {
		installDependentOn(Example.EXAMPLE_1, Example1RootBundle.class);
		installDependentOn(Example.EXAMPLE_2, Example2Bundle.class);
	}

	@Override
	public void bootstrap(Bootstrapper bootstrap) {
		bootstrap.install(getClass(), Example.class);
		// just so that even without any example we have the env available
		bootstrap.install(DefaultFeature.ENV);
	}
}
